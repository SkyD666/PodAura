package com.skyd.podaura.ui.player.coordinator

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.KeyEvent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import co.touchlab.kermit.Logger
import com.skyd.fundation.di.inject
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean.Companion.articleId
import com.skyd.podaura.model.bean.playlist.mergePlaylistOrder
import com.skyd.podaura.model.playlist.PlaylistPlayback
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.preference.player.PlayerLoopModePreference
import com.skyd.podaura.model.repository.player.IPlayerRepository
import com.skyd.podaura.ui.PlatformSurfaceHolder
import com.skyd.podaura.ui.player.LoopMode
import com.skyd.podaura.ui.player.PlaybackEnd
import com.skyd.podaura.ui.player.PlaybackEndReason
import com.skyd.podaura.ui.player.PlaybackFailure
import com.skyd.podaura.ui.player.PlayerCommand
import com.skyd.podaura.ui.player.PlayerEvent
import com.skyd.podaura.ui.player.PlayerLaunchData
import com.skyd.podaura.ui.player.PlayerOpenRequest
import com.skyd.podaura.ui.player.PlayerViewModel
import com.skyd.podaura.ui.player.SleepTimerOption
import com.skyd.podaura.ui.player.externalPlaybackError
import com.skyd.podaura.ui.player.mpv.EventListener
import com.skyd.podaura.ui.player.mpv.MPV
import com.skyd.podaura.ui.player.mpv.MPVEvent
import com.skyd.podaura.ui.player.mpv.MPVFormat
import com.skyd.podaura.ui.player.mpv.MPVPlayer
import com.skyd.podaura.ui.player.mpv.PlayerKeyInput
import com.skyd.podaura.ui.player.mpv.configurePlaylistHeaders
import com.skyd.podaura.ui.player.mpv.mapPlayerKeyEvent
import com.skyd.podaura.ui.player.mpv.resizeSurface
import com.skyd.podaura.ui.player.playbackIntent
import com.skyd.podaura.ui.player.preparePlatformPlayback
import com.skyd.podaura.ui.player.playerTrace
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.update
import kotlin.math.pow
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid

class PlayerCoordinator : LifecycleOwner, PlaylistPlayback {
    override val lifecycle = LifecycleRegistry(this)
    private val logger = Logger.withTag("PlayerCoordinator")
    private val playerRepo: IPlayerRepository by inject()
    private val player = MPVPlayer.instance
    private val progress by lazy {
        PlaybackProgress(playerRepo) { error ->
            logger.w(throwable = error) { "Failed to persist playback history" }
        }
    }

    val sleepTimer get() = sleepTimerController.state

    val model = PlayerModel()
    val playerState get() = model.playerState
    private val _playlistLoads = MutableSharedFlow<Unit>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    internal val playlistLoads = _playlistLoads.asSharedFlow()
    internal val renderPlayer: MPV get() = player.mpv

    private val _engineState = MutableStateFlow<PlayerEngineState>(PlayerEngineState.Initializing)
    val engineState = _engineState.asStateFlow()
    private val _preparationState = MutableStateFlow(PlaybackPreparation.Idle)
    val preparationState = _preparationState.asStateFlow()
    private var retryPreparation: (() -> Unit)? = null

    fun open(request: PlayerOpenRequest, viewModel: PlayerViewModel) {
        if (request == PlayerOpenRequest.Resume || destroyed.load()) return
        retryPreparation = { open(request, viewModel) }
        _preparationState.value = PlaybackPreparation.Loading
        val onPrepared: (PlayerLaunchData) -> Unit = { data ->
            if (!destroyed.load()) {
                onCommand(data.toLoadCommand())
                _preparationState.value = PlaybackPreparation.Idle
            }
        }
        val onFailure: (Throwable) -> Unit = { error ->
            logger.e(throwable = error) { "Failed to prepare playback" }
            if (!destroyed.load()) _preparationState.value = PlaybackPreparation.Failed
        }
        when (request) {
            is PlayerOpenRequest.Media -> viewModel.handlePlayDataMode(
                request.mode, request.requestId, onPrepared, onFailure,
            )

            is PlayerOpenRequest.Files -> viewModel.handlePlatformFiles(
                request.files, request.requestId, onPrepared, onFailure,
            )

            PlayerOpenRequest.Resume -> Unit
        }
    }

    fun playOrRetry() {
        when {
            _preparationState.value == PlaybackPreparation.Failed -> retryPreparation?.invoke()
            _engineState.value is PlayerEngineState.Failed -> onCommand(PlayerCommand.RetryInitialize)
            else -> onCommand(PlayerCommand.PlayOrPause)
        }
    }

    private val engineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO.limitedParallelism(1)
    )
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val destroyed = AtomicBoolean(false)
    private val activeGeneration = AtomicReference<Long?>(null)
    private val nativeMediaGeneration = AtomicReference(0L)
    private val commands = Channel<ActorMessage>(Channel.UNLIMITED)
    private val retrySignal = Channel<Unit>(Channel.CONFLATED)
    private val loadSignal = Channel<Unit>(Channel.CONFLATED)
    private val telemetrySignal = Channel<Unit>(Channel.CONFLATED)
    private val latestLoad = AtomicReference<PendingLoad?>(null)
    private val latestTelemetry = AtomicReference(Telemetry())
    private val pendingTransform = AtomicReference(Transform())
    private val transformScheduled = AtomicBoolean(false)
    private val observers = AtomicReference<Set<Observer>>(emptySet())

    // Confined to the engine dispatcher. Timer actions also run while the actor awaits I/O;
    // playback history remains actor-owned.
    private val endOfMediaPause = EndOfMediaPause(player)
    private val sleepTimerController = PlayerSleepTimer(player, engineScope, endOfMediaPause) {
        emitEvent(PlayerEvent.Paused(true))
        mainScope.launch { onPlaybackCommand?.invoke(PlayerCommand.Paused(true), false) }
        commands.trySend(ActorMessage.SaveProgress)
    }
    private var externalPlayback: ExternalPlaybackSession? = null
    private var playlistId = ""
    private var queueId = ""
    private val cachedPlaylistMap = linkedMapOf<String, PlaylistMediaWithArticleBean>()
    private var pendingStartPosition: PendingStartPosition? = null
    private var lastLoadRequestId: String? = null
    private var currentPath: String? = null
    private var currentMediaGeneration = 0L
    private var hasMediaReady = false
    private val liveSurfaces = linkedSetOf<PlatformSurfaceHolder>()
    private var activeSurface: PlatformSurfaceHolder? = null
    private val playbackKeysDown = mutableSetOf<String>()

    private val engineJob: Job

    init {
        lifecycle.currentState = Lifecycle.State.CREATED
        engineJob = engineScope.launch {
            player.withOwnership { generation ->
                activeGeneration.store(generation)
                var observer: EventListener? = null
                try {
                    initializeWithRetry()
                    if (destroyed.load()) return@withOwnership
                    observer = createMpvObserver(generation)
                    player.mpv.addEventListener(observer)
                    player.mpv.observeProperty("eof-reached", MPVFormat.MPV_FORMAT_FLAG)
                    setLoopMode(
                        PlayerLoopModePreference.toLoopMode(
                            dataStore.getOrDefault(PlayerLoopModePreference)
                        )
                    )
                    _engineState.value = PlayerEngineState.AwaitingMedia
                    withContext(Dispatchers.Main.immediate) {
                        lifecycle.currentState = Lifecycle.State.RESUMED
                    }
                    actorLoop()
                } finally {
                    activeGeneration.store(null)
                    withContext(NonCancellable) {
                        observer?.let { runCatching { player.mpv.removeEventListener(it) } }
                        withContext(Dispatchers.Main.immediate) {
                            onDetachAll(this@PlayerCoordinator)
                        }
                        sleepTimerController.close()
                        detachNativeSurface()
                        progress.finish()
                        emitShutdown()
                        playerTrace("Player/MpvDestroy") { player.destroy() }
                        externalPlayback?.release()
                        externalPlayback = null
                        latestLoad.exchange(null)?.command?.externalBatch?.release()
                        _engineState.value = PlayerEngineState.Destroyed
                        withContext(Dispatchers.Main.immediate) {
                            lifecycle.currentState = Lifecycle.State.DESTROYED
                        }
                        ioScope.cancel()
                        mainScope.cancel()
                    }
                }
            }
        }
    }

    fun interface Observer {
        fun onEvent(event: PlayerEvent)
    }

    fun addObserver(observer: Observer) = observers.update { it + observer }
    fun removeObserver(observer: Observer) = observers.update { it - observer }
    fun removeAllObserver() = observers.store(emptySet())

    override suspend fun withQueueOrder(
        playlistId: String,
        expectedOrder: List<String>?,
        update: suspend (applyOrder: (List<String>) -> Unit) -> Unit,
    ): Boolean {
        if (destroyed.load() || !_engineState.value.isReady) return false
        val result = CompletableDeferred<Boolean>()
        val completion = engineJob.invokeOnCompletion {
            result.completeExceptionally(CancellationException("Player closed"))
        }
        try {
            commands.send(
                ActorMessage.UpdateQueueOrder(playlistId, expectedOrder, update, result)
            )
            return result.await()
        } finally {
            completion.dispose()
        }
    }

    override suspend fun synchronizePlaylist(
        playlistId: String,
        readPlaylist: suspend () -> List<PlaylistMediaWithArticleBean>,
    ) {
        if (destroyed.load()) return
        val result = CompletableDeferred<Unit>()
        val completion = engineJob.invokeOnCompletion {
            result.completeExceptionally(CancellationException("Player closed"))
        }
        try {
            commands.send(ActorMessage.SynchronizePlaylist(playlistId, readPlaylist, result))
            result.await()
        } finally {
            completion.dispose()
        }
    }

    internal var onPlaybackCommand: ((PlayerCommand, Boolean?) -> Unit)? = null

    /** Runs in the engine queue before playback, without blocking the calling UI. */
    internal var preparePlaybackCommand: (suspend (Boolean?) -> Boolean)? = null

    fun setSleepTimer(option: SleepTimerOption) {
        postEngineAction { sleepTimerController.set(option) }
    }

    internal fun refreshSleepTimer() {
        postEngineAction { sleepTimerController.refresh() }
    }

    private fun postEngineAction(action: () -> Unit) {
        val generation = activeGeneration.load() ?: return
        if (!_engineState.value.isReady || destroyed.load()) return
        engineScope.launch {
            if (!destroyed.load() && activeGeneration.load() == generation) action()
        }
    }

    internal fun setSystemPaused(paused: Boolean) {
        if (_engineState.value.isReady && !destroyed.load()) {
            if (paused) postEngineAction { sleepTimerController.finishFadeOnPause() }
            commands.trySend(ActorMessage.SystemPause(paused))
        }
    }

    fun onCommand(command: PlayerCommand) {
        // Snapshot before querying native playback intent, which can straddle expiry.
        val submittedTimerPauseGeneration = sleepTimerController.pendingPauseGeneration()
        // Resolve skips once, before enqueueing: keep-open's EOF pause is not a user pause.
        // The policy, audio activation and engine all consume this same playback intent.
        val skipping = command == PlayerCommand.NextMedia || command == PlayerCommand.PreviousMedia
        val intent = if (skipping && _engineState.value.isReady && !destroyed.load()) {
            command.playbackIntent(player.paused, player.eofReached)
        } else command.playbackIntent(playerState.value.paused)
        onPlaybackCommand?.invoke(command, intent)
        if (intent == false) postEngineAction { sleepTimerController.finishFadeOnPause() }
        when (command) {
            PlayerCommand.Destroy -> destroy()
            PlayerCommand.RetryInitialize -> {
                if (_engineState.value is PlayerEngineState.Failed) retrySignal.trySend(Unit)
            }

            is PlayerCommand.LoadList -> {
                if (!destroyed.load()) {
                    if (command.externalBatch?.retain() == false) return
                    latestLoad.exchange(PendingLoad(command, submittedTimerPauseGeneration))
                        ?.command?.externalBatch?.release()
                    if (destroyed.load()) latestLoad.exchange(null)?.command?.externalBatch?.release()
                    loadSignal.trySend(Unit)
                }
            }

            is PlayerCommand.Attach -> {
                if (_engineState.value.isReady) {
                    onAttach(this, command.surfaceHolder, ::postSurfaceEvent)
                }
            }

            is PlayerCommand.Detach -> {
                onDetach(this, command.surfaceHolder)
                postSurfaceEvent(PlayerSurfaceEvent.Destroyed(command.surfaceHolder))
            }

            is PlayerCommand.Rotate -> postTransform { copy(rotate = command.rotate) }
            is PlayerCommand.Zoom -> postTransform { copy(zoom = command.zoom) }
            is PlayerCommand.VideoOffset -> postTransform { copy(offset = command.offset) }
            else -> {
                val retryingFirstMedia = command == PlayerCommand.PlayOrPause &&
                        _engineState.value == PlayerEngineState.LoadingMedia &&
                        playerState.value.lastPlaybackEnd?.reason == PlaybackEndReason.Error
                if ((_engineState.value.isReady || retryingFirstMedia) && !destroyed.load()) {
                    commands.trySend(
                        ActorMessage.Command(
                            command,
                            intent,
                            submittedTimerPauseGeneration
                        )
                    )
                }
            }
        }
    }

    fun onPlaybackFailureHandled(id: String, retry: Boolean = false) {
        if (!destroyed.load()) {
            commands.trySend(ActorMessage.PlaybackFailureHandled(id, retry))
        }
    }

    fun onKey(event: KeyEvent): Boolean {
        if (!_engineState.value.isReady || destroyed.load()) return false
        val input = mapPlayerKeyEvent(event, logger) ?: return false
        if (input.action != null && input.key != null) onNativeKey(input)
        return true
    }

    internal fun onNativeKey(input: PlayerKeyInput) {
        val submittedTimerPauseGeneration = sleepTimerController.pendingPauseGeneration()
        postEngineAction {
            val key = input.key ?: return@postEngineAction
            if (input.action == "keyup" && playbackKeysDown.remove(key)) return@postEngineAction
            val command = player.mpv.playbackCommandForKey(key)
            if (command == null) {
                player.dispatchKey(input)
                return@postEngineAction
            }
            if (input.action == "keydown") {
                if (!playbackKeysDown.add(key)) return@postEngineAction
            } else if (input.action != "keypress") return@postEngineAction
            val intent = command.playbackIntent(player.paused, player.eofReached)
            mainScope.launch {
                onPlaybackCommand?.invoke(command, intent)
                commands.trySend(
                    ActorMessage.Command(
                        command,
                        intent,
                        submittedTimerPauseGeneration
                    )
                )
            }
            if (intent == false) sleepTimerController.finishFadeOnPause()
        }
    }

    private suspend fun initializeWithRetry() {
        while (currentCoroutineContext().isActive && !destroyed.load()) {
            _engineState.value = PlayerEngineState.Initializing
            try {
                player.ensureInitialized()
                return
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                logger.e(throwable = error) { "Failed to initialize mpv" }
                _engineState.value = PlayerEngineState.Failed(
                    error.message ?: "Player initialization failed"
                )
                retrySignal.receive()
            }
        }
    }

    private fun destroy() {
        if (!destroyed.compareAndSet(expectedValue = false, newValue = true)) return
        onDetachAll(this)
        sleepTimerController.cancelJob()
        engineJob.cancel()
    }

    // Wait for history, native resources and lifecycle cleanup, including cancellation
    // before the engine acquires playback ownership.
    internal suspend fun awaitDestroyed() = engineJob.join()

    private suspend fun actorLoop() {
        var running = true
        while (running && currentCoroutineContext().isActive && !destroyed.load()) {
            select<Unit> {
                commands.onReceive { running = handleMessage(it) }
                loadSignal.onReceive { latestLoad.exchange(null)?.let { loadList(it) } }
                telemetrySignal.onReceive { flushTelemetry() }
            }
        }
    }

    private suspend fun handleMessage(message: ActorMessage): Boolean {
        when (message) {
            is ActorMessage.UpdateQueueOrder -> updateQueueOrder(message)
            is ActorMessage.SynchronizePlaylist -> synchronizePlaylist(message)
            is ActorMessage.Command -> executeCommand(
                message.command, message.playbackIntent, message.submittedTimerPauseGeneration,
            )

            is ActorMessage.SystemPause -> applySystemPause(message.paused)
            is ActorMessage.EndFile -> handleEndFile(
                reason = message.reason,
                mpvError = message.mpvError,
                playlistEntryId = message.playlistEntryId,
            )

            is ActorMessage.NativeEvent -> return handleNativeEvent(message)
            is ActorMessage.Property -> handleProperty(message)
            is ActorMessage.Surface -> handleSurface(message.event)
            is ActorMessage.PlaybackFailureHandled -> {
                val failure = model.consumePlaybackFailure(message.id)
                if (message.retry && failure?.retryEnd != null &&
                    playerState.value.lastPlaybackEnd == failure.retryEnd && _engineState.value.isReady
                ) {
                    handleFailedPlaybackRetry()
                }
            }

            ActorMessage.FlushTransform -> flushTransform()
            ActorMessage.SaveProgress -> progress.save()
        }
        return true
    }

    private suspend fun applySystemPause(paused: Boolean) {
        sleepTimerController.checkDeadline()
        if (!paused && sleepTimerController.paused) return
        if (preparePlaybackCommand?.invoke(!paused) == false) return
        // Audio-session activation may suspend past the timer deadline.
        sleepTimerController.checkDeadline()
        if (!paused && sleepTimerController.paused) return
        setPaused(paused)
    }

    private suspend fun executeCommand(
        command: PlayerCommand, playbackIntent: Boolean?, submittedTimerPauseGeneration: Long?,
    ) {
        sleepTimerController.checkDeadline()
        if (playbackIntent == true) {
            clearTimerPause(command, submittedTimerPauseGeneration)
        }
        if (preparePlaybackCommand?.invoke(playbackIntent) == false) return
        when (command) {
            is PlayerCommand.RemoveMediaFromPlaylist -> removeMedia(command)
            is PlayerCommand.Paused -> setPaused(command.paused)

            PlayerCommand.PlayOrPause -> setPaused(playbackIntent != true)

            PlayerCommand.PreviousMedia -> {
                if (player.playlistCount > 1 && (player.playlistPos > 0 || player.isIdling ||
                            playerState.value.loop == LoopMode.LoopPlaylist)
                ) {
                    sleepTimerController.mediaChanging()
                }
                progress.save()
                endOfMediaPause.skip(
                    next = false, play = playbackIntent == true && !sleepTimerController.paused,
                    loopPlaylist = playerState.value.loop == LoopMode.LoopPlaylist,
                )
            }

            PlayerCommand.NextMedia -> {
                if (player.playlistCount > 1 && (player.playlistPos < player.playlistCount - 1 ||
                            playerState.value.loop == LoopMode.LoopPlaylist)
                ) {
                    sleepTimerController.mediaChanging()
                }
                progress.save()
                endOfMediaPause.skip(
                    next = true, play = playbackIntent == true && !sleepTimerController.paused,
                    loopPlaylist = playerState.value.loop == LoopMode.LoopPlaylist,
                )
            }

            is PlayerCommand.SeekTo -> seekTo(command.position)

            is PlayerCommand.AudioDelay -> player.audioDelay(command.delayMillis)
            is PlayerCommand.SubtitleDelay -> player.subtitleDelay(command.delayMillis)
            is PlayerCommand.SetSpeed -> player.playbackSpeed = command.speed.toDouble()
            is PlayerCommand.SetSubtitleTrack -> player.sid = command.trackId
            is PlayerCommand.SetAudioTrack -> player.aid = command.trackId
            is PlayerCommand.Screenshot -> player.screenshot(command.onSaveScreenshot)
            is PlayerCommand.AddSubtitle -> player.addSubtitle(command.filePath)
            is PlayerCommand.AddAudio -> player.addAudio(command.filePath)
            is PlayerCommand.Shuffle -> player.shuffle(command.shuffle)
            is PlayerCommand.SetLoopMode -> setLoopMode(command.mode)
            PlayerCommand.CycleLoop -> {
                val entries = LoopMode.entries
                setLoopMode(entries[(playerState.value.loop.ordinal + 1) % entries.size])
            }

            is PlayerCommand.PlayFileInPlaylist -> {
                if (command.path != player.path && command.path in player.loadPlaylist()) {
                    sleepTimerController.mediaChanging()
                }
                progress.save()
                player.playFileInPlaylist(command.path)
                if (sleepTimerController.paused) player.paused = true
            }

            is PlayerCommand.Attach,
            is PlayerCommand.Detach,
            is PlayerCommand.LoadList,
            PlayerCommand.Destroy,
            PlayerCommand.RetryInitialize,
            is PlayerCommand.Rotate,
            is PlayerCommand.Zoom,
            is PlayerCommand.VideoOffset -> Unit
        }
    }

    private suspend fun clearTimerPause(
        command: PlayerCommand,
        submittedTimerPauseGeneration: Long?
    ) {
        if (!sleepTimerController.resume(submittedTimerPauseGeneration)) return
        // Expiry may have overwritten the intent reported when this command was enqueued.
        withContext(Dispatchers.Main.immediate) {
            onPlaybackCommand?.invoke(command, true)
        }
    }

    private fun handleFailedPlaybackRetry(): Boolean {
        val end = playerState.value.lastPlaybackEnd
            ?.takeIf { it.reason == PlaybackEndReason.Error }
            ?: return false
        if (!player.retryPlaylistEntry(end.playlistEntryId, end.path)) {
            logger.w {
                "Failed playlist entry is no longer available; clearing playback error " +
                        "(entryId=${end.playlistEntryId}, path=${end.path})"
            }
            emitEvent(PlayerEvent.ClearPlaybackEnd)
        }
        return true
    }

    private suspend fun removeMedia(command: PlayerCommand.RemoveMediaFromPlaylist) {
        if (playlistId != command.playlist.firstOrNull()?.playlistMediaBean?.playlistId) return
        if (command.playlist.any { it.playlistMediaBean.url == player.path }) {
            sleepTimerController.mediaChanging()
        }
        progress.save()
        command.playlist.forEach { cachedPlaylistMap.remove(it.playlistMediaBean.url) }
        player.removeFromList(command.playlist.map { it.playlistMediaBean.url })
        externalPlayback?.retainPaths(cachedPlaylistMap.keys)
    }

    private suspend fun synchronizePlaylist(request: ActorMessage.SynchronizePlaylist) {
        if (request.result.isCompleted) return
        if (request.playlistId != playlistId || playlistId.isEmpty()) {
            request.result.complete(Unit)
            return
        }
        // A desktop full load can await a render surface. Do not compute edits from the old
        // native queue or enqueue duplicate appends behind it. Leave the actor free for attach,
        // detach and source changes, then read the latest database state after that load settles.
        if (player.mpv.hasPendingCommands) {
            engineScope.launch {
                delay(50)
                if (!destroyed.load() && !request.result.isCompleted) commands.send(request)
            }
            return
        }
        try {
            val source = request.readPlaylist()
            val original = player.loadPlaylist()
            val existing = original.mapNotNull(cachedPlaylistMap::get)
                .associateBy { it.playlistMediaBean.stableUrl }
            val additions = source.filterNot { it.playlistMediaBean.stableUrl in existing }
            val prepared = preparePlatformPlayback(
                PlayerLaunchData(null, additions, requestId = Uuid.random().toString())
            )
            prepared.externalBatch?.let { batch ->
                if (externalPlayback == null) externalPlayback = ExternalPlaybackSession(batch)
                else externalPlayback?.add(batch)
                batch.failures.takeIf { it.isNotEmpty() }?.let { failures ->
                    emitEvent(PlayerEvent.PlaybackFailed(PlaybackFailure(
                        details = failures.joinToString("\n") { "${it.source}: ${it.reason}" },
                    )))
                }
            }
            val added = prepared.playlist.associateBy { it.playlistMediaBean.stableUrl }
            val desired = source.mapNotNull { item ->
                // Retain the native path (e.g. an iOS provider URL) and its existing access lease.
                existing[item.playlistMediaBean.stableUrl]?.copy(article = item.article)
                    ?: added[item.playlistMediaBean.stableUrl]
            }
            val paths = desired.map { it.playlistMediaBean.url }
            if (player.path in original && player.path !in paths) {
                sleepTimerController.mediaChanging()
                progress.save()
            }
            cachedPlaylistMap.putAll(desired.map { it.playlistMediaBean.url to it })
            player.mpv.configurePlaylistHeaders(desired)
            player.synchronizePlaylist(paths)
            cachedPlaylistMap.keys.retainAll(paths.toSet())
            externalPlayback?.retainPaths(paths.toSet())
            if (sleepTimerController.paused) player.paused = true
            emitPlaylist()
            emitEvent(PlayerEvent.PlaylistPosition(player.playlistPos))
            request.result.complete(Unit)
        } catch (error: Throwable) {
            // The source is already committed. Report failure and retain access to any entries
            // actually present after a partial native update; later source changes can retry.
            val actual = player.loadPlaylist().toSet()
            cachedPlaylistMap.keys.retainAll(actual)
            externalPlayback?.retainPaths(actual)
            emitPlaylist()
            emitEvent(PlayerEvent.PlaylistPosition(player.playlistPos))
            if (error !is CancellationException) {
                emitEvent(PlayerEvent.PlaybackFailed(PlaybackFailure(details = error.message)))
            }
            request.result.completeExceptionally(error)
            if (error is CancellationException) throw error
        }
    }

    private suspend fun updateQueueOrder(request: ActorMessage.UpdateQueueOrder) {
        if (request.result.isCancelled) return
        if (request.playlistId != playlistId) {
            request.result.complete(false)
            return
        }
        val original = player.loadPlaylist()
        try {
            check(!player.shuffle) { "Turn off shuffle before reordering" }
            val stableToPath = original.associateBy { it.toStableMediaUrl() }
            val applyOrder: (List<String>) -> Unit = { order ->
                val paths = order.mapNotNull(stableToPath::get)
                player.reorderPlaylist(mergePlaylistOrder(original, paths))
            }
            request.expectedOrder?.let { expected ->
                check(expected.size == original.size && expected.toSet() == stableToPath.keys) {
                    "The queue changed during the drag"
                }
            }
            request.update(applyOrder)
            emitPlaylist()
            emitEvent(PlayerEvent.PlaylistPosition(player.playlistPos))
            request.result.complete(true)
        } catch (error: Throwable) {
            // The caller owns the operation; undo native moves if it cannot complete.
            runCatching { player.reorderPlaylist(original) }.onFailure { error.addSuppressed(it) }
            emitPlaylist()
            emitEvent(PlayerEvent.PlaylistPosition(player.playlistPos))
            request.result.completeExceptionally(error)
            if (error is CancellationException) throw error
        }
    }

    private suspend fun loadList(load: PendingLoad) {
        val command = load.command
        var adopted = false
        try {
            command.externalBatch?.failures?.takeIf { it.isNotEmpty() }?.let {
                emitEvent(
                    PlayerEvent.PlaybackFailed(
                        PlaybackFailure(
                            details = it.joinToString("\n") { failure -> "${failure.source}: ${failure.reason}" },
                        )
                    )
                )
            }
            if (command.playlist.isEmpty()) return
            sleepTimerController.checkDeadline()
            clearTimerPause(command, load.submittedTimerPauseGeneration)
            if (preparePlaybackCommand?.invoke(true) == false) return
            loadPreparedList(command)
            externalPlayback?.release()
            externalPlayback = command.externalBatch?.let(::ExternalPlaybackSession)
            adopted = true
            // Even a same-source load may contain a snapshot prepared before a committed edit.
            // Let the playlist owner reconcile it after this load and resource adoption finish.
            _playlistLoads.tryEmit(Unit)
        } finally {
            if (!adopted) command.externalBatch?.release()
        }
    }

    private suspend fun loadPreparedList(command: PlayerCommand.LoadList) {
        val files = command.playlist.map { it.playlistMediaBean.url }
        val targetPath = command.startPath?.takeIf { it in files } ?: files.firstOrNull()
        if (targetPath != player.path) sleepTimerController.mediaChanging()
        if (!hasMediaReady) _engineState.value = PlayerEngineState.LoadingMedia
        val isNewRequest = shouldConsumeLoadRequest(command.requestId, lastLoadRequestId)
        val startPositionSeconds = command.startPositionSeconds?.takeIf { isNewRequest }
        if (isNewRequest && command.requestId != null) {
            lastLoadRequestId = command.requestId
        }
        val seekCurrentMedia = startPositionSeconds != null && shouldSeekCurrentMedia(
            startPositionSeconds = startPositionSeconds,
            startPath = command.startPath,
            currentPath = player.path,
            currentPlaylist = player.loadPlaylist(),
            requestedPlaylist = files,
        )
        val replayedStartPosition = command.startPositionSeconds != null &&
                startPositionSeconds == null
        if (!replayedStartPosition) {
            pendingStartPosition = startPositionSeconds
                ?.takeUnless { seekCurrentMedia }
                ?.let { position -> command.startPath?.let { PendingStartPosition(it, position) } }
        }
        progress.save()
        val newPlaylistId = command.playlist.firstOrNull()?.playlistMediaBean?.playlistId.orEmpty()
        // Saved sources identify their own queue. Temporary sources need an identity that changes
        // only on replacement, not on reordering, removing an item or advancing playback.
        if (newPlaylistId.isNotEmpty()) queueId = newPlaylistId
        else if (playlistId.isNotEmpty() || files.toSet() != cachedPlaylistMap.keys) {
            queueId = Uuid.random().toString()
        }
        playlistId = newPlaylistId
        cachedPlaylistMap.clear()
        cachedPlaylistMap.putAll(command.playlist.map { it.playlistMediaBean.url to it })
        // External multi-file opens must advance normally, even on desktop's keep-open=always.
        endOfMediaPause.setExternalQueue(command.externalBatch != null)
        if (isNewRequest) emitEvent(PlayerEvent.ClearPlaybackEnd)
        player.mpv.configurePlaylistHeaders(command.playlist)
        player.loadList(files = files, startFile = command.startPath)
        // Switching between saved sources with identical native contents produces no playlist
        // event. Publish that source change too, once the requested contents are actually loaded.
        if (player.loadPlaylist() == files) {
            emitPlaylist()
            emitEvent(PlayerEvent.PlaylistPosition(player.playlistPos))
        }
        if (sleepTimerController.paused) player.paused = true
        startPositionSeconds?.takeIf { seekCurrentMedia }?.let { seekAndPlay(it) }
        if (isNewRequest) setPaused(false)
    }

    private suspend fun setPaused(paused: Boolean) {
        if (paused && sleepTimerController.finishFadeOnPause()) return
        if (!paused && sleepTimerController.paused) return
        if (!paused && handleFailedPlaybackRetry()) return
        if (!paused) {
            if ((endOfMediaPause.enabled || player.keepOpen) && player.eofReached) seekTo(0)
            else if (player.isIdling && player.playlistCount > 0) {
                player.playMediaAtIndex(player.playlistCount - 1)
            }
        }
        // Seeking from EOF can suspend in history I/O while the timer finishes.
        if (!paused && sleepTimerController.paused) return
        player.paused = paused
        if (paused) progress.save()
    }

    private fun createMpvObserver(generation: Long) = object : EventListener {
        private fun isCurrent() = activeGeneration.load() == generation && !destroyed.load()

        override fun onPropertyChange(name: String) {
            if (!isCurrent()) return
            when (name) {
                "playlist" -> postTelemetry { copy(playlistDirty = true) }
                "track-list" -> postTelemetry { copy(tracksDirty = true) }
                else -> commands.trySend(ActorMessage.Property(name = name))
            }
        }

        override fun onPropertyChange(name: String, value: Long) {
            if (!isCurrent()) return
            commands.trySend(ActorMessage.Property(name = name, longValue = value))
        }

        override fun onPropertyChange(name: String, value: Boolean) {
            if (!isCurrent()) return
            if (name in LOADING_PROPERTIES) postTelemetry { copy(loadingDirty = true) }
            else commands.trySend(ActorMessage.Property(name = name, booleanValue = value))
        }

        override fun onPropertyChange(name: String, value: String) {
            if (isCurrent()) {
                commands.trySend(ActorMessage.Property(name = name, stringValue = value))
            }
        }

        override fun onPropertyChange(name: String, value: Double) {
            if (!isCurrent()) return
            when (name) {
                "demuxer-cache-duration" -> postTelemetry { copy(buffer = value.toInt()) }
                "video-zoom" -> postTelemetry { copy(zoom = value) }
                "video-pan-x" -> postTelemetry { copy(panX = value) }
                "video-pan-y" -> postTelemetry { copy(panY = value) }
                else -> commands.trySend(
                    ActorMessage.Property(name = name, doubleValue = value)
                )
            }
        }

        override fun onEndFile(reason: Int, mpvError: Int, playlistEntryId: Long) {
            if (isCurrent()) {
                commands.trySend(ActorMessage.EndFile(reason, mpvError, playlistEntryId))
            }
        }

        override fun onEvent(event: Int) {
            if (!isCurrent()) return
            if (event == MPVEvent.START_FILE) nativeMediaGeneration.update { it + 1 }
            commands.trySend(
                ActorMessage.NativeEvent(
                    event = event,
                    mediaGeneration = nativeMediaGeneration.load(),
                    path = if (event == MPVEvent.START_FILE) player.path else null,
                    position = if (event == MPVEvent.PLAYBACK_RESTART) player.timePos.toLong() else 0L,
                    duration = if (event == MPVEvent.FILE_LOADED) player.duration.toLong() else 0L,
                    seekable = event == MPVEvent.FILE_LOADED && player.mpv.getPropertyBoolean("seekable"),
                )
            )
        }
    }

    private suspend fun handleNativeEvent(message: ActorMessage.NativeEvent): Boolean {
        flushTelemetry()
        when (message.event) {
            MPVEvent.SEEK -> {
                progress.beginSeek()
                emitEvent(PlayerEvent.Seek)
            }

            MPVEvent.START_FILE -> {
                sleepTimerController.mediaStarted(message.path)
                progress.finish()
                currentPath = message.path
                currentMediaGeneration = message.mediaGeneration
                if (!hasMediaReady) _engineState.value = PlayerEngineState.LoadingMedia
                emitEvent(PlayerEvent.StartFile(currentPath))
                emitEvent(PlayerEvent.Loading(true))
            }

            MPVEvent.END_FILE -> {
                handleEndFile(
                    reason = UNKNOWN_END_REASON,
                    mpvError = 0,
                    playlistEntryId = UNKNOWN_PLAYLIST_ENTRY_ID,
                )
            }

            MPVEvent.FILE_LOADED -> onFileLoaded(message.duration, message.seekable)
            MPVEvent.PLAYBACK_RESTART -> {
                if (!player.eofReached) sleepTimerController.restoreEndOfMediaOptions()
                progress.restarted(message.position)
                emitEvent(PlayerEvent.PlaybackRestart)
                emitEvent(PlayerEvent.Paused(player.paused))
            }

            MPVEvent.SHUTDOWN -> return false
        }
        return true
    }

    private suspend fun handleEndFile(
        reason: Int,
        mpvError: Int,
        playlistEntryId: Long,
    ) {
        val mappedReason = PlaybackEndReason.fromMpv(reason)
        // The engine may already have advanced by the time the actor consumes this event.
        val endedPath = if (externalPlayback != null && playlistEntryId >= 0) {
            player.playlistEntryPath(playlistEntryId) ?: return
        } else currentPath
        val end = PlaybackEnd(
            reason = mappedReason,
            errorCode = mpvError.takeIf { mappedReason == PlaybackEndReason.Error },
            playlistEntryId = playlistEntryId.takeIf { it >= 0L },
            path = endedPath,
        )
        if (mappedReason == PlaybackEndReason.Eof) sleepTimerController.mediaEnded(endedPath)
        emitEvent(PlayerEvent.EndFile(end))
        if (mappedReason == PlaybackEndReason.Error) {
            logger.e {
                "Playback failed (error=$mpvError, entryId=$playlistEntryId, " +
                        "path=$endedPath)"
            }
            end.toPlaybackFailure(
                autoAdvance = externalPlayback != null,
                source = endedPath?.toStableMediaUrl(),
            )?.let { emitEvent(PlayerEvent.PlaybackFailed(it)) }
            if (endedPath != null &&
                externalPlayback?.recordFailure(endedPath, cachedPlaylistMap.keys) == true
            ) {
                player.stop()
            }
        }
        emitEvent(PlayerEvent.Loading(false))
        progress.finish()
        currentPath = null
    }

    private suspend fun onFileLoaded(duration: Long, seekable: Boolean) {
        sleepTimerController.checkDeadline()
        if (sleepTimerController.paused) player.paused = true
        sleepTimerController.restoreEndOfMediaOptions()
        val loadedPath = currentPath
        externalPlayback?.onFileLoaded(loadedPath)
        // A coalesced track-list update can be consumed before StartFile clears the model.
        // Resnapshot after loading so portrait mode does not remain on the audio placeholder.
        player.loadTracks()
        emitEvent(
            PlayerEvent.FileLoaded(
                videoTracks = player.videoTrack,
                audioTracks = player.audioTrack,
                subtitleTracks = player.subtitleTrack,
                videoTrackId = player.vid,
                audioTrackId = player.aid,
                subtitleTrackId = player.sid,
            )
        )
        hasMediaReady = true
        _engineState.value = PlayerEngineState.Ready
        emitEvent(PlayerEvent.Paused(player.paused))
        emitEvent(PlayerEvent.Loading(player.loading()))
        val startPosition = pendingStartPosition
            .also { pendingStartPosition = null }
            ?.takeIf { it.path == loadedPath }
        loadedPath?.let { path ->
            val position = progress.start(
                path = path.toHistoryUrl(),
                duration = duration,
                articleId = cachedPlaylistMap[path]?.articleId,
                startPosition = startPosition?.positionSeconds,
                fallbackPath = path.toStableMediaUrl().takeUnless { it == path.toHistoryUrl() },
                seekable = seekable,
            )
            // Native playback can advance while the actor is waiting for the database.
            if (nativeMediaGeneration.load() != currentMediaGeneration) return@let
            if (seekable && (position > 0 || startPosition != null)) {
                endOfMediaPause.seek(position.toInt())
                progress.seek(position)
            } else progress.save()
            if (startPosition != null && !sleepTimerController.paused) player.paused = false
        }
        emitEvent(PlayerEvent.MediaThumbnail(player.thumbnail))
    }

    private suspend fun handleProperty(property: ActorMessage.Property) {
        with(property) {
            when (name) {
                "time-pos" -> longValue?.let {
                    progress.update(it)
                    emitEvent(PlayerEvent.Position(it))
                }

                "aid" -> emitEvent(PlayerEvent.AudioTrackChanged(longValue?.toInt() ?: player.aid))
                "sid" -> emitEvent(
                    PlayerEvent.SubtitleTrackChanged(
                        longValue?.toInt() ?: player.sid
                    )
                )

                "vid" -> emitEvent(PlayerEvent.VideoTrackChanged(longValue?.toInt() ?: player.vid))
                "duration" -> longValue?.let {
                    progress.updateDuration(it)
                    emitEvent(PlayerEvent.Duration(it))
                }

                "video-rotate" -> longValue?.let { emitEvent(PlayerEvent.Rotate(it.toFloat())) }
                "playlist-pos" -> longValue?.let {
                    emitEvent(PlayerEvent.PlaylistPosition(it.toInt()))
                }

                "pause" -> booleanValue?.let {
                    if (it) progress.save()
                    emitEvent(PlayerEvent.Paused(it))
                }

                "eof-reached" -> if (booleanValue == true && player.eofReached) {
                    sleepTimerController.mediaEnded(player.path)
                }

                "seekable" -> booleanValue?.let { emitEvent(PlayerEvent.Seekable(it)) }
                "shuffle" -> booleanValue?.let { emitEvent(PlayerEvent.Shuffle(it)) }
                "idle-active" -> booleanValue?.let { emitEvent(PlayerEvent.Idling(it)) }
                "media-title" -> stringValue?.let { emitEvent(PlayerEvent.MediaTitle(it)) }
                "speed" -> doubleValue?.let { emitEvent(PlayerEvent.Speed(it.toFloat())) }
                "audio-delay" -> doubleValue?.let {
                    emitEvent(PlayerEvent.AudioDelay((it * 1000).toLong()))
                }

                "sub-delay" -> doubleValue?.let {
                    emitEvent(PlayerEvent.SubtitleDelay((it * 1000).toLong()))
                }

                "loop-file", "loop-playlist" -> updateLoopMode()
                "metadata" -> {
                    emitEvent(PlayerEvent.Artist(player.artist))
                    emitEvent(PlayerEvent.Album(player.album))
                }
            }
        }
    }

    private fun updateLoopMode() {
        if (endOfMediaPause.enabled) return
        val mode = when {
            player.loopPlaylist -> LoopMode.LoopPlaylist
            player.loopOne -> LoopMode.LoopFile
            else -> LoopMode.None
        }
        emitEvent(PlayerEvent.Loop(mode))
        PlayerLoopModePreference.put(ioScope, mode)
    }

    private fun flushTelemetry() {
        val telemetry = latestTelemetry.exchange(Telemetry())
        telemetry.buffer?.let { emitEvent(PlayerEvent.Buffer(it)) }
        telemetry.zoom?.let { emitEvent(PlayerEvent.Zoom(2.0.pow(it).toFloat())) }
        telemetry.panX?.let {
            emitEvent(PlayerEvent.VideoOffsetX((it * player.videoDW).toFloat()))
        }
        telemetry.panY?.let {
            emitEvent(PlayerEvent.VideoOffsetY((it * player.videoDH).toFloat()))
        }
        if (telemetry.loadingDirty) emitEvent(PlayerEvent.Loading(player.loading()))
        if (telemetry.tracksDirty) {
            player.loadTracks()
            emitEvent(PlayerEvent.AllSubtitleTracks(player.subtitleTrack))
            emitEvent(PlayerEvent.AllVideoTracks(player.videoTrack))
            emitEvent(PlayerEvent.AllAudioTracks(player.audioTrack))
        }
        if (telemetry.playlistDirty) emitPlaylist()
    }

    private fun emitPlaylist() {
        val playlistMap = LinkedHashMap<String, PlaylistMediaWithArticleBean>()
        player.loadPlaylist().forEachIndexed { index, url ->
            playlistMap[url] = cachedPlaylistMap[url]
                ?: PlaylistMediaWithArticleBean.fromUrl(
                    playlistId = playlistId,
                    url = url,
                    orderPosition = index.toDouble(),
                )
        }
        emitEvent(PlayerEvent.Playlist(playlistId, playlistMap, queueId))
    }

    private fun postTelemetry(update: Telemetry.() -> Telemetry) {
        latestTelemetry.update(update)
        telemetrySignal.trySend(Unit)
    }

    private fun postTransform(update: Transform.() -> Transform) {
        if (!_engineState.value.isReady || destroyed.load()) return
        pendingTransform.update(update)
        scheduleTransform()
    }

    private fun scheduleTransform() {
        if (transformScheduled.compareAndSet(expectedValue = false, newValue = true)) {
            engineScope.launch {
                delay(TRANSFORM_FRAME_MILLIS.milliseconds)
                commands.send(ActorMessage.FlushTransform)
            }
        }
    }

    private fun flushTransform() {
        val transform = pendingTransform.exchange(Transform())
        transform.rotate?.let(player::rotate)
        transform.zoom?.let(player::zoom)
        transform.offset?.let { player.offset(it.x.toInt(), it.y.toInt()) }
        transformScheduled.store(false)
        if (pendingTransform.load() != Transform()) scheduleTransform()
    }

    private fun postSurfaceEvent(event: PlayerSurfaceEvent) {
        if (!destroyed.load()) commands.trySend(ActorMessage.Surface(event))
    }

    private fun handleSurface(event: PlayerSurfaceEvent) {
        when (event) {
            is PlayerSurfaceEvent.Created -> {
                liveSurfaces += event.holder
                attachNativeSurface(event.holder)
            }

            is PlayerSurfaceEvent.Changed -> {
                if (event.holder == activeSurface) {
                    player.mpv.resizeSurface(event.width, event.height)
                }
            }

            is PlayerSurfaceEvent.Destroyed -> {
                liveSurfaces -= event.holder
                if (event.holder == activeSurface) {
                    detachNativeSurface()
                    liveSurfaces.lastOrNull()?.let(::attachNativeSurface)
                }
            }
        }
    }

    private fun attachNativeSurface(holder: PlatformSurfaceHolder) {
        if (activeSurface == holder) return
        player.mpv.attachSurface(holder)
        player.mpv.option("force-window", "yes")
        player.mpv.setPropertyString("vo", player.voInUse)
        activeSurface = holder
    }

    private fun detachNativeSurface() {
        if (activeSurface == null || !player.initialized) return
        runCatching {
            player.mpv.setPropertyString("vo", "null")
            player.mpv.option("force-window", "no")
            player.mpv.detachSurface()
        }
        activeSurface = null
    }

    private fun setLoopMode(mode: LoopMode) {
        endOfMediaPause.setLoopMode(mode)
        emitEvent(PlayerEvent.Loop(mode))
        PlayerLoopModePreference.put(ioScope, mode)
    }

    private suspend fun seekTo(positionSeconds: Long) {
        if (nativeMediaGeneration.load() != currentMediaGeneration) return
        if (!player.mpv.getPropertyBoolean("seekable")) return
        val duration = player.duration.toLong()
        val position = positionSeconds.coerceAtLeast(0L).let {
            if (duration > 0) it.coerceAtMost(duration) else it
        }
        endOfMediaPause.seek(position.toInt())
        progress.seek(position)
    }

    private suspend fun seekAndPlay(positionSeconds: Long) {
        seekTo(positionSeconds)
        if (!sleepTimerController.paused) player.paused = false
    }

    private fun emitEvent(event: PlayerEvent) {
        model.onEvent(event)
        mainScope.launch {
            observers.load().forEach { it.onEvent(event) }
        }
    }

    private suspend fun emitShutdown() {
        model.onEvent(PlayerEvent.Shutdown)
        withContext(Dispatchers.Main.immediate) {
            observers.load().forEach { it.onEvent(PlayerEvent.Shutdown) }
            removeAllObserver()
        }
    }

    private fun String.toStableMediaUrl(): String =
        cachedPlaylistMap[this]?.playlistMediaBean?.stableUrl ?: this

    private fun String.toHistoryUrl(): String =
        cachedPlaylistMap[this]?.playlistMediaBean?.historyUrl ?: toStableMediaUrl()

    private sealed interface ActorMessage {
        data class SynchronizePlaylist(
            val playlistId: String,
            val readPlaylist: suspend () -> List<PlaylistMediaWithArticleBean>,
            val result: CompletableDeferred<Unit>,
        ) : ActorMessage

        data class UpdateQueueOrder(
            val playlistId: String,
            val expectedOrder: List<String>?,
            val update: suspend (applyOrder: (List<String>) -> Unit) -> Unit,
            val result: CompletableDeferred<Boolean>,
        ) : ActorMessage

        data class Command(
            val command: PlayerCommand,
            val playbackIntent: Boolean?,
            val submittedTimerPauseGeneration: Long?,
        ) : ActorMessage

        data class SystemPause(val paused: Boolean) : ActorMessage
        data class EndFile(
            val reason: Int,
            val mpvError: Int,
            val playlistEntryId: Long,
        ) : ActorMessage

        data class NativeEvent(
            val event: Int,
            val mediaGeneration: Long,
            val path: String?,
            val position: Long,
            val duration: Long,
            val seekable: Boolean,
        ) : ActorMessage

        data class Property(
            val name: String,
            val longValue: Long? = null,
            val booleanValue: Boolean? = null,
            val stringValue: String? = null,
            val doubleValue: Double? = null,
        ) : ActorMessage

        data class Surface(val event: PlayerSurfaceEvent) : ActorMessage
        data class PlaybackFailureHandled(val id: String, val retry: Boolean) : ActorMessage
        data object FlushTransform : ActorMessage
        data object SaveProgress : ActorMessage
    }

    private data class Telemetry(
        val buffer: Int? = null,
        val zoom: Double? = null,
        val panX: Double? = null,
        val panY: Double? = null,
        val loadingDirty: Boolean = false,
        val tracksDirty: Boolean = false,
        val playlistDirty: Boolean = false,
    )

    private data class Transform(
        val rotate: Int? = null,
        val zoom: Float? = null,
        val offset: Offset? = null,
    )

    private data class PendingStartPosition(
        val path: String,
        val positionSeconds: Long,
    )

    private data class PendingLoad(
        val command: PlayerCommand.LoadList,
        val submittedTimerPauseGeneration: Long?,
    )

    private companion object {
        const val UNKNOWN_END_REASON = -1
        const val UNKNOWN_PLAYLIST_ENTRY_ID = -1L
        const val TRANSFORM_FRAME_MILLIS = 16L
        val LOADING_PROPERTIES = setOf("paused-for-cache", "core-idle", "demuxer-cache-idle")
    }
}

internal fun shouldSeekCurrentMedia(
    startPositionSeconds: Long?,
    startPath: String?,
    currentPath: String?,
    currentPlaylist: List<String>,
    requestedPlaylist: List<String>,
): Boolean = startPositionSeconds != null &&
        startPath == currentPath && currentPlaylist == requestedPlaylist

internal fun shouldConsumeLoadRequest(requestId: String?, lastRequestId: String?): Boolean =
    requestId == null || requestId != lastRequestId

internal fun PlaybackEnd.toPlaybackFailure(
    autoAdvance: Boolean,
    source: String? = path,
): PlaybackFailure? {
    if (reason != PlaybackEndReason.Error) return null
    return if (autoAdvance) {
        source?.let { PlaybackFailure(details = "$it: ${externalPlaybackError(errorCode ?: 0)}") }
    } else PlaybackFailure(retryEnd = this)
}

enum class PlaybackPreparation { Idle, Loading, Failed }
