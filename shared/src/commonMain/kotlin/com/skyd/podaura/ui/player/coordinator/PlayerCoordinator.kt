package com.skyd.podaura.ui.player.coordinator

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.KeyEvent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import co.touchlab.kermit.Logger
import com.skyd.fundation.di.inject
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.model.bean.playlist.MediaUrlWithArticleIdBean.Companion.toMediaUrlWithArticleIdBean
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean.Companion.articleId
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.preference.player.PlayerLoopModePreference
import com.skyd.podaura.model.repository.player.IPlayerRepository
import com.skyd.podaura.model.repository.playlist.IAddToPlaylistRepository
import com.skyd.podaura.ui.PlatformSurfaceHolder
import com.skyd.podaura.ui.player.LoopMode
import com.skyd.podaura.ui.player.PlaybackEnd
import com.skyd.podaura.ui.player.PlaybackEndReason
import com.skyd.podaura.ui.player.PlaybackFailure
import com.skyd.podaura.ui.player.PlayerCommand
import com.skyd.podaura.ui.player.PlayerEvent
import com.skyd.podaura.ui.player.externalPlaybackError
import com.skyd.podaura.ui.player.mpv.EventListener
import com.skyd.podaura.ui.player.mpv.MPV
import com.skyd.podaura.ui.player.mpv.MPVEvent
import com.skyd.podaura.ui.player.mpv.MPVPlayer
import com.skyd.podaura.ui.player.mpv.PlayerKeyInput
import com.skyd.podaura.ui.player.mpv.mapPlayerKeyEvent
import com.skyd.podaura.ui.player.playerTrace
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.update
import kotlin.math.abs
import kotlin.math.pow
import kotlin.time.Duration.Companion.milliseconds

class PlayerCoordinator : LifecycleOwner {
    override val lifecycle = LifecycleRegistry(this)
    private val logger = Logger.withTag("PlayerCoordinator")
    private val playerRepo: IPlayerRepository by inject()
    private val addToPlaylistRepo: IAddToPlaylistRepository by inject()
    private val player = MPVPlayer.instance

    val model = PlayerModel()
    val playerState get() = model.playerState
    internal val renderPlayer: MPV get() = player.mpv

    private val _engineState = MutableStateFlow<PlayerEngineState>(PlayerEngineState.Initializing)
    val engineState = _engineState.asStateFlow()

    private val engineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO.limitedParallelism(1)
    )
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val destroyed = AtomicBoolean(false)
    private val activeGeneration = AtomicReference<Long?>(null)
    private val commands = Channel<ActorMessage>(Channel.UNLIMITED)
    private val retrySignal = Channel<Unit>(Channel.CONFLATED)
    private val loadSignal = Channel<Unit>(Channel.CONFLATED)
    private val telemetrySignal = Channel<Unit>(Channel.CONFLATED)
    private val latestLoad = AtomicReference<PlayerCommand.LoadList?>(null)
    private val latestTelemetry = AtomicReference(Telemetry())
    private val pendingTransform = AtomicReference(Transform())
    private val transformScheduled = AtomicBoolean(false)
    private val observers = AtomicReference<Set<Observer>>(emptySet())

    // The fields below belong to the engine actor.
    private var externalPlayback: ExternalPlaybackSession? = null
    private var playlistId = ""
    private val cachedPlaylistMap = linkedMapOf<String, PlaylistMediaWithArticleBean>()
    private var pendingStartPosition: PendingStartPosition? = null
    private var lastLoadRequestId: String? = null
    private var currentPath: String? = null
    private var currentPathPlayed = false
    private var hasMediaReady = false
    private var lastPositionSeconds = 0L
    private val liveSurfaces = linkedSetOf<PlatformSurfaceHolder>()
    private var activeSurface: PlatformSurfaceHolder? = null

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
                        detachNativeSurface()
                        savePosition(currentPath?.toStableMediaUrl())
                        emitShutdown()
                        playerTrace("Player/MpvDestroy") { player.destroy() }
                        externalPlayback?.release()
                        externalPlayback = null
                        latestLoad.exchange(null)?.externalBatch?.release()
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

    fun onCommand(command: PlayerCommand) {
        when (command) {
            PlayerCommand.Destroy -> destroy()
            PlayerCommand.RetryInitialize -> {
                if (_engineState.value is PlayerEngineState.Failed) retrySignal.trySend(Unit)
            }

            is PlayerCommand.LoadList -> {
                if (!destroyed.load()) {
                    if (command.externalBatch?.retain() == false) return
                    latestLoad.exchange(command)?.externalBatch?.release()
                    if (destroyed.load()) latestLoad.exchange(null)?.externalBatch?.release()
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
                if (_engineState.value.isReady && !destroyed.load()) {
                    commands.trySend(ActorMessage.Command(command))
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
        if (input.action != null && input.key != null) {
            commands.trySend(ActorMessage.Key(input))
        }
        return true
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
        engineJob.cancel()
    }

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
            is ActorMessage.Command -> executeCommand(message.command)
            is ActorMessage.Key -> player.dispatchKey(message.input)
            is ActorMessage.EndFile -> handleEndFile(
                reason = message.reason,
                mpvError = message.mpvError,
                playlistEntryId = message.playlistEntryId,
            )

            is ActorMessage.NativeEvent -> return handleNativeEvent(message.event)
            is ActorMessage.Property -> handleProperty(message)
            is ActorMessage.Surface -> handleSurface(message.event)
            is ActorMessage.ApplyLastPosition -> applyLastPosition(message)
            is ActorMessage.PlaybackFailureHandled -> {
                val failure = model.consumePlaybackFailure(message.id)
                if (message.retry && failure?.retryEnd != null &&
                    playerState.value.lastPlaybackEnd == failure.retryEnd && _engineState.value.isReady
                ) {
                    handleFailedPlaybackRetry()
                }
            }

            ActorMessage.FlushTransform -> flushTransform()
        }
        return true
    }

    private fun executeCommand(command: PlayerCommand) {
        when (command) {
            is PlayerCommand.RemoveMediaFromPlaylist -> removeMedia(command)
            is PlayerCommand.Paused -> setPaused(command.paused)

            PlayerCommand.PlayOrPause -> {
                if (!handleFailedPlaybackRetry()) player.cyclePause()
            }

            PlayerCommand.PreviousMedia -> player.playlistPrev()
            PlayerCommand.NextMedia -> player.playlistNext()
            is PlayerCommand.SeekTo -> player.seek(
                command.position.coerceIn(
                    0L,
                    player.duration.toLong().coerceAtLeast(0L),
                ).toInt()
            )

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

            is PlayerCommand.PlayFileInPlaylist -> player.playFileInPlaylist(command.path)
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

    private fun removeMedia(command: PlayerCommand.RemoveMediaFromPlaylist) {
        if (playlistId != command.playlist.firstOrNull()?.playlistMediaBean?.playlistId) return
        command.playlist.forEach { cachedPlaylistMap.remove(it.playlistMediaBean.url) }
        externalPlayback?.retainPaths(cachedPlaylistMap.keys)
        val currentPlaylistId = playlistId
        ioScope.launch {
            addToPlaylistRepo.removeMediaFromPlaylist(
                playlistId = currentPlaylistId,
                mediaList = command.playlist.map { it.toMediaUrlWithArticleIdBean() },
            ).collect()
        }
        player.removeFromList(command.playlist.map { it.playlistMediaBean.url })
    }

    private suspend fun loadList(command: PlayerCommand.LoadList) {
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
            loadPreparedList(command)
            externalPlayback?.release()
            externalPlayback = command.externalBatch?.let(::ExternalPlaybackSession)
            adopted = true
        } finally {
            if (!adopted) command.externalBatch?.release()
        }
    }

    private suspend fun loadPreparedList(command: PlayerCommand.LoadList) {
        val files = command.playlist.map { it.playlistMediaBean.url }
        if (!hasMediaReady) _engineState.value = PlayerEngineState.LoadingMedia
        val isNewRequest = shouldConsumeLoadRequest(command.requestId, lastLoadRequestId)
        val startPositionSeconds = command.startPositionSeconds?.takeIf { isNewRequest }
        if (isNewRequest && command.requestId != null) {
            lastLoadRequestId = command.requestId
        }
        val seekCurrentMedia = shouldSeekCurrentMedia(
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
        if (currentPathPlayed && (externalPlayback != null || command.externalBatch != null)) {
            // Save while the old engine URL still maps to its original content URI.
            savePosition(currentPath?.toStableMediaUrl())
        }
        playlistId = command.playlist.firstOrNull()?.playlistMediaBean?.playlistId.orEmpty()
        cachedPlaylistMap.clear()
        cachedPlaylistMap.putAll(command.playlist.map { it.playlistMediaBean.url to it })
        // External multi-file opens must advance normally, even on desktop's keep-open=always.
        player.setExternalQueue(command.externalBatch != null)
        if (isNewRequest) emitEvent(PlayerEvent.ClearPlaybackEnd)
        player.loadList(files = files, startFile = command.startPath)
        startPositionSeconds?.takeIf { seekCurrentMedia }?.let(::seekAndPlay)
        if (isNewRequest) setPaused(false)
    }

    private fun setPaused(paused: Boolean) {
        if (!paused && handleFailedPlaybackRetry()) return
        if (!paused) {
            if (player.keepOpen && player.eofReached) player.seek(0)
            else if (player.isIdling && player.playlistCount > 0) {
                player.playMediaAtIndex(player.playlistCount - 1)
            }
        }
        player.paused = paused
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
            if (name == "time-pos") postTelemetry { copy(position = value) }
            else commands.trySend(ActorMessage.Property(name = name, longValue = value))
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
            if (isCurrent()) commands.trySend(ActorMessage.NativeEvent(event))
        }
    }

    private suspend fun handleNativeEvent(event: Int): Boolean {
        flushTelemetry()
        when (event) {
            MPVEvent.SEEK -> emitEvent(PlayerEvent.Seek)
            MPVEvent.START_FILE -> {
                currentPath = player.path
                currentPathPlayed = false
                lastPositionSeconds = 0L
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

            MPVEvent.FILE_LOADED -> onFileLoaded()
            MPVEvent.PLAYBACK_RESTART -> {
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
        if (currentPathPlayed) savePosition(currentPath?.toStableMediaUrl())
        currentPath = null
        currentPathPlayed = false
    }

    private fun onFileLoaded() {
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
        currentPathPlayed = true
        hasMediaReady = true
        _engineState.value = PlayerEngineState.Ready
        val duration = player.duration.toLong()
        loadedPath?.let { path ->
            val stablePath = path.toStableMediaUrl()
            val articleId = cachedPlaylistMap[path]?.articleId
            ioScope.launch {
                playerRepo.insertPlayHistory(stablePath, duration, articleId).collect()
            }
        }
        emitEvent(PlayerEvent.Paused(player.paused))
        emitEvent(PlayerEvent.Loading(player.loading()))
        val startPosition = pendingStartPosition
            .also { pendingStartPosition = null }
            ?.takeIf { it.path == loadedPath }
        if (startPosition != null) seekAndPlay(startPosition.positionSeconds)
        else loadLastPosition(loadedPath?.toStableMediaUrl())
        emitEvent(PlayerEvent.MediaThumbnail(player.thumbnail))
    }

    private fun handleProperty(property: ActorMessage.Property) {
        with(property) {
            when (name) {
                "aid" -> emitEvent(PlayerEvent.AudioTrackChanged(longValue?.toInt() ?: player.aid))
                "sid" -> emitEvent(
                    PlayerEvent.SubtitleTrackChanged(
                        longValue?.toInt() ?: player.sid
                    )
                )

                "vid" -> emitEvent(PlayerEvent.VideoTrackChanged(longValue?.toInt() ?: player.vid))
                "duration" -> longValue?.let { emitEvent(PlayerEvent.Duration(it)) }
                "video-rotate" -> longValue?.let { emitEvent(PlayerEvent.Rotate(it.toFloat())) }
                "playlist-pos" -> longValue?.let {
                    emitEvent(PlayerEvent.PlaylistPosition(it.toInt()))
                }

                "pause" -> booleanValue?.let { emitEvent(PlayerEvent.Paused(it)) }
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
        telemetry.position?.let {
            lastPositionSeconds = it
            emitEvent(PlayerEvent.Position(it))
        }
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
        emitEvent(PlayerEvent.Playlist(playlistId, playlistMap))
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
                    player.mpv.setPropertyString(
                        "android-surface-size",
                        "${event.width}x${event.height}",
                    )
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
        when (mode) {
            LoopMode.LoopPlaylist -> player.loopPlaylist()
            LoopMode.LoopFile -> player.loopFile()
            LoopMode.None -> player.loopNo()
        }
    }

    private fun seekAndPlay(positionSeconds: Long) {
        player.seek(
            positionSeconds.coerceIn(
                0L,
                player.duration.toLong().coerceAtLeast(0L),
            ).toInt()
        )
        player.paused = false
    }

    private fun loadLastPosition(path: String?) {
        if (path == null) return
        ioScope.launch {
            val lastPosition = playerRepo.requestLastPlayPosition(path).first()
            commands.send(ActorMessage.ApplyLastPosition(path, lastPosition))
        }
    }

    private fun applyLastPosition(message: ActorMessage.ApplyLastPosition) {
        if (currentPath?.toStableMediaUrl() != message.path) return
        if (message.positionMillis > 0 &&
            abs(player.duration - message.positionMillis / 1000) > 20
        ) {
            player.seek((message.positionMillis / 1000).toInt().coerceAtLeast(0))
        }
    }

    private suspend fun savePosition(path: String?) {
        if (path == null) return
        val positionMillis = lastPositionSeconds * 1000L
        if (positionMillis <= 1000L) return
        try {
            playerRepo.updateLastPlayPosition(path, positionMillis).collect()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            logger.w(throwable = error) { "Failed to save playback position" }
        }
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

    private sealed interface ActorMessage {
        data class Command(val command: PlayerCommand) : ActorMessage
        data class Key(val input: PlayerKeyInput) : ActorMessage
        data class EndFile(
            val reason: Int,
            val mpvError: Int,
            val playlistEntryId: Long,
        ) : ActorMessage

        data class NativeEvent(val event: Int) : ActorMessage
        data class Property(
            val name: String,
            val longValue: Long? = null,
            val booleanValue: Boolean? = null,
            val stringValue: String? = null,
            val doubleValue: Double? = null,
        ) : ActorMessage

        data class Surface(val event: PlayerSurfaceEvent) : ActorMessage
        data class ApplyLastPosition(val path: String, val positionMillis: Long) : ActorMessage
        data class PlaybackFailureHandled(val id: String, val retry: Boolean) : ActorMessage
        data object FlushTransform : ActorMessage
    }

    private data class Telemetry(
        val position: Long? = null,
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
