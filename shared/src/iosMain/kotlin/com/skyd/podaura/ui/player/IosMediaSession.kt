package com.skyd.podaura.ui.player

import co.touchlab.kermit.Logger
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.SuccessResult
import coil3.size.Precision
import coil3.size.Scale
import coil3.toBitmap
import com.skyd.podaura.ext.flowOf
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.preference.player.BackgroundPlayPreference
import com.skyd.podaura.ui.component.imageLoaderBuilder
import com.skyd.podaura.ui.component.imageRequest
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.player.coordinator.isReady
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.impl.use
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionInterruptionNotification
import platform.AVFAudio.AVAudioSessionInterruptionOptionKey
import platform.AVFAudio.AVAudioSessionInterruptionOptionShouldResume
import platform.AVFAudio.AVAudioSessionInterruptionTypeBegan
import platform.AVFAudio.AVAudioSessionInterruptionTypeEnded
import platform.AVFAudio.AVAudioSessionInterruptionTypeKey
import platform.AVFAudio.AVAudioSessionMediaServicesWereResetNotification
import platform.AVFAudio.AVAudioSessionRouteChangeNotification
import platform.AVFAudio.AVAudioSessionRouteChangeReasonKey
import platform.AVFAudio.AVAudioSessionRouteChangeReasonOldDeviceUnavailable
import platform.AVFAudio.AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
import platform.AVFAudio.setActive
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSOperationQueue
import platform.Foundation.create
import platform.MediaPlayer.MPChangePlaybackPositionCommandEvent
import platform.MediaPlayer.MPMediaItemArtwork
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyArtwork
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoPropertyDefaultPlaybackRate
import platform.MediaPlayer.MPNowPlayingInfoPropertyElapsedPlaybackTime
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import platform.MediaPlayer.MPRemoteCommand
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.MediaPlayer.MPRemoteCommandEvent
import platform.MediaPlayer.MPRemoteCommandHandlerStatusCommandFailed
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess
import platform.MediaPlayer.MPSkipIntervalCommandEvent
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationProtectedDataDidBecomeAvailable
import platform.UIKit.UIApplicationProtectedDataWillBecomeUnavailable
import platform.UIKit.UIApplicationState.UIApplicationStateBackground
import platform.UIKit.UIImage

// AVAudioSession is shared across player lifetimes. Keep activation and teardown ordered.
private val audioSessionDispatcher = Dispatchers.IO.limitedParallelism(1)

/** App-lifetime system integration; never owned by the full-screen composable. */
internal class IosMediaSession(
    private val coordinator: PlayerCoordinator,
    private val onArtwork: (UIImage?) -> Unit,
) : AutoCloseable {
    private val audio = AVAudioSession.sharedInstance()
    private val center = NSNotificationCenter.defaultCenter
    private val remote = MPRemoteCommandCenter.sharedCommandCenter()
    private val nowPlaying = MPNowPlayingInfoCenter.defaultCenter()
    private val imageLoader = lazy { PlatformContext.INSTANCE.imageLoaderBuilder().build() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val commands = mutableListOf<Pair<MPRemoteCommand, Any>>()
    private val observers = mutableListOf<Any>()
    private val resumePolicy = PlaybackResumePolicy(
        initiallyBackground = UIApplication.sharedApplication.applicationState == UIApplicationStateBackground,
        initiallyScreenLocked = !UIApplication.sharedApplication.protectedDataAvailable,
    )
    private var artworkSource: Any? = null
    private var artwork: MPMediaItemArtwork? = null
    private var artworkJob: Job? = null

    private var systemCommand = false
    private var audioActivationGeneration = 0L
    private var requestedAudioActivationGeneration: Long? = null
    private var closed = false

    init {
        coordinator.onPlaybackCommand = ::userCommand
        coordinator.preparePlaybackCommand = ::preparePlaybackCommand
        // Activate when a playback command arrives, rather than blocking player presentation.
        command(remote.playCommand) { send(PlayerCommand.Paused(false)) }
        command(remote.pauseCommand) { send(PlayerCommand.Paused(true)) }
        command(remote.togglePlayPauseCommand) { send(PlayerCommand.PlayOrPause) }
        command(remote.nextTrackCommand) { send(PlayerCommand.NextMedia) }
        command(remote.previousTrackCommand) { send(PlayerCommand.PreviousMedia) }
        command(remote.changePlaybackPositionCommand) { event ->
            send(PlayerCommand.SeekTo((event as MPChangePlaybackPositionCommandEvent).positionTime.toLong()))
        }
        remote.skipForwardCommand.preferredIntervals = listOf(NSNumber(int = 30))
        remote.skipBackwardCommand.preferredIntervals = listOf(NSNumber(int = 10))
        command(remote.skipForwardCommand) { event ->
            send(PlayerCommand.SeekTo(coordinator.playerState.value.position + (event as MPSkipIntervalCommandEvent).interval.toLong()))
        }
        command(remote.skipBackwardCommand) { event ->
            send(PlayerCommand.SeekTo(coordinator.playerState.value.position - (event as MPSkipIntervalCommandEvent).interval.toLong()))
        }
        observe(UIApplicationDidEnterBackgroundNotification) {
            if (resumePolicy.enterBackground(
                    !coordinator.playerState.value.paused,
                    dataStore.getOrDefault(BackgroundPlayPreference)
                )
            ) systemPause(true)
            updateNowPlaying()
        }
        observe(UIApplicationDidBecomeActiveNotification) {
            setScreenLocked(false)
            if (resumePolicy.enterForeground()) {
                systemPause(false)
            }
            updateNowPlaying()
        }
        observe(UIApplicationProtectedDataWillBecomeUnavailable) { setScreenLocked(true) }
        observe(UIApplicationProtectedDataDidBecomeAvailable) { setScreenLocked(false) }
        observe(AVAudioSessionInterruptionNotification) { notification ->
            val info = notification.userInfo.orEmpty()
            val type = (info[AVAudioSessionInterruptionTypeKey] as? NSNumber)?.unsignedIntegerValue
            if (type == AVAudioSessionInterruptionTypeBegan) {
                audioActivationGeneration++
                resumePolicy.beginInterruption(!coordinator.playerState.value.paused)
                systemPause(true)
            } else if (type == AVAudioSessionInterruptionTypeEnded) {
                val options =
                    (info[AVAudioSessionInterruptionOptionKey] as? NSNumber)?.unsignedIntegerValue
                        ?: 0u
                val resume = resumePolicy.endInterruption(
                    options and AVAudioSessionInterruptionOptionShouldResume != 0uL,
                    dataStore.getOrDefault(BackgroundPlayPreference)
                )
                if (resume) {
                    systemPause(false)
                }
            }
        }
        observe(AVAudioSessionRouteChangeNotification) { notification ->
            if ((notification.userInfo?.get(AVAudioSessionRouteChangeReasonKey) as? NSNumber)?.unsignedIntegerValue == AVAudioSessionRouteChangeReasonOldDeviceUnavailable) {
                send(PlayerCommand.Paused(true))
            }
        }
        observe(AVAudioSessionMediaServicesWereResetNotification) {
            if (!coordinator.playerState.value.paused) scope.launch {
                if (!activateAudio()) systemPause(true)
            }
        }
        scope.launch {
            combine(
                coordinator.playerState,
                coordinator.engineState,
                dataStore.flowOf(BackgroundPlayPreference),
            ) { state, engine, backgroundEnabled -> Triple(state, engine, backgroundEnabled) }
                .collect { (state, engine, backgroundEnabled) ->
                    if (resumePolicy.pauseWhenRequired(
                            !state.paused,
                            engine.isReady,
                            backgroundEnabled
                        )
                    ) {
                        systemPause(true)
                    }
                    val source = state.currentMedia?.thumbnail
                        ?: state.mediaThumbnail
                        ?: state.currentMedia?.thumbnailAny
                    if (source != artworkSource) {
                        artworkSource = source
                        artwork = null
                        onArtwork(null)
                        artworkJob?.cancel()
                        if (source != null) artworkJob = launch {
                            val image = loadIosArtwork(source, imageLoader.value)
                            if (image != null && artworkSource == source) {
                                artwork = MPMediaItemArtwork(image.size) { image }
                                onArtwork(image)
                                updateNowPlaying()
                            }
                        }
                    }
                    updateNowPlaying()
                }
        }
    }

    private fun setScreenLocked(locked: Boolean) {
        if (resumePolicy.setScreenLocked(
                locked,
                !coordinator.playerState.value.paused,
                dataStore.getOrDefault(BackgroundPlayPreference),
            )
        ) {
            systemPause(locked)
        }
        updateNowPlaying()
    }

    fun setPictureInPictureActive(active: Boolean) {
        if (resumePolicy.setPictureInPictureActive(active)) {
            systemPause(false)
        }
        if (resumePolicy.pauseWhenRequired(
                !coordinator.playerState.value.paused,
                coordinator.engineState.value.isReady,
                dataStore.getOrDefault(BackgroundPlayPreference)
            )
        ) systemPause(true)
        updateNowPlaying()
    }

    /** UI commands clear pending system resumes even if pause was already true. */
    private fun systemPause(paused: Boolean) {
        systemCommand = true
        try {
            coordinator.onCommand(PlayerCommand.Paused(paused))
        } finally {
            systemCommand = false
        }
    }

    fun userCommand(command: PlayerCommand) {
        if (systemCommand) return
        command.playbackIntent(coordinator.playerState.value.paused)?.let { allowsResume ->
            audioActivationGeneration++
            requestedAudioActivationGeneration = audioActivationGeneration.takeIf { allowsResume }
            resumePolicy.userAction(allowsResume = allowsResume)
        }
    }

    private suspend fun preparePlaybackCommand(command: PlayerCommand): Boolean {
        if (command.playbackIntent(coordinator.playerState.value.paused) != true) return true
        return withContext(Dispatchers.Main.immediate) {
            if (closed) return@withContext false
            val generation = requestedAudioActivationGeneration
            val activated = activateAudio()
            if (closed) return@withContext false
            if (activated && generation != null && generation == audioActivationGeneration) {
                resumePolicy.audioSessionActivated()
            }
            activated
        }
    }

    private fun send(command: PlayerCommand) {
        coordinator.onCommand(command)
    }

    private fun updateNowPlaying() {
        val state = coordinator.playerState.value
        UIApplication.sharedApplication.idleTimerDisabled =
            state.mediaStarted && !resumePolicy.background && !state.paused && state.isVideo
        if (!state.mediaStarted) {
            nowPlaying.nowPlayingInfo = null; return
        }
        val info = mutableMapOf<Any?, Any?>(
            MPMediaItemPropertyTitle to (state.mediaTitle ?: state.currentMedia?.title.orEmpty()),
            MPMediaItemPropertyArtist to (state.artist ?: state.currentMedia?.artist.orEmpty()),
            MPMediaItemPropertyPlaybackDuration to NSNumber(double = state.duration.toDouble()),
            MPNowPlayingInfoPropertyElapsedPlaybackTime to NSNumber(double = state.position.toDouble()),
            MPNowPlayingInfoPropertyPlaybackRate to NSNumber(double = if (state.paused) 0.0 else state.speed.toDouble()),
            MPNowPlayingInfoPropertyDefaultPlaybackRate to NSNumber(double = state.speed.toDouble()),
        )
        artwork?.let { info[MPMediaItemPropertyArtwork] = it }
        nowPlaying.nowPlayingInfo = info
        remote.changePlaybackPositionCommand.enabled = state.seekable
    }

    private suspend fun activateAudio(): Boolean = withContext(audioSessionDispatcher) {
        playerTrace("Player/AudioSessionActivate") {
            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                if ((audio.category != AVAudioSessionCategoryPlayback &&
                            !audio.setCategory(AVAudioSessionCategoryPlayback, error.ptr)) ||
                    !audio.setActive(true, error.ptr)
                ) {
                    Logger.w(
                        "Audio session: ${error.value?.localizedDescription}",
                        tag = "IosMediaSession"
                    )
                    false
                } else {
                    true
                }
            }
        }
    }

    private fun command(command: MPRemoteCommand, block: (MPRemoteCommandEvent) -> Unit) {
        command.enabled = true
        val token = command.addTargetWithHandler { event ->
            if (event == null) MPRemoteCommandHandlerStatusCommandFailed else {
                scope.launch { block(event) }
                MPRemoteCommandHandlerStatusSuccess
            }
        }
        commands += command to token
    }

    private fun observe(name: String?, block: (NSNotification) -> Unit) {
        observers += center.addObserverForName(name, null, NSOperationQueue.mainQueue) {
            it?.let(
                block
            )
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        coordinator.onPlaybackCommand = null
        coordinator.preparePlaybackCommand = null
        observers.forEach(center::removeObserver)
        commands.forEach { (command, token) ->
            command.removeTarget(token); command.enabled = false
        }
        scope.cancel()
        if (imageLoader.isInitialized()) imageLoader.value.shutdown()
        nowPlaying.nowPlayingInfo = null
        UIApplication.sharedApplication.idleTimerDisabled = false
        // This job must survive cancellation of the player session above.
        CoroutineScope(audioSessionDispatcher).launch {
            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                if (!audio.setActive(
                        false,
                        AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation,
                        error.ptr
                    )
                ) {
                    Logger.w(
                        "Audio session deactivation: ${error.value?.localizedDescription}",
                        tag = "IosMediaSession"
                    )
                }
            }
        }
    }
}

/** Share the player's Coil fetchers, decoders and disk cache with the native artwork layer. */
internal suspend fun loadIosArtwork(source: Any, imageLoader: ImageLoader): UIImage? =
    withContext(Dispatchers.IO) {
        val request = imageRequest(source, PlatformContext.INSTANCE).newBuilder()
            .size(1024, 1024)
            .scale(Scale.FIT)
            .precision(Precision.INEXACT)
            .build()
        val result = imageLoader.execute(request) as? SuccessResult ?: return@withContext null
        Image.makeFromBitmap(result.image.toBitmap()).use { image ->
            image.encodeToData(EncodedImageFormat.PNG, 100)?.use { data ->
                val bytes = data.bytes
                bytes.usePinned {
                    UIImage.imageWithData(NSData.create(it.addressOf(0), bytes.size.toULong()))
                }
            }
        }
    }
