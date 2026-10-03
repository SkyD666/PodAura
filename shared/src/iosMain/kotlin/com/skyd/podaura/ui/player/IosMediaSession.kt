package com.skyd.podaura.ui.player

import co.touchlab.kermit.Logger
import com.skyd.podaura.ext.flowOf
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.preference.player.BackgroundPlayPreference
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.player.coordinator.isReady
import com.skyd.podaura.util.coil.localmedia.getLocalMediaThumbnailData
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.create
import platform.Foundation.dataTaskWithURL
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
import platform.UIKit.UIApplicationState.UIApplicationStateBackground
import platform.UIKit.UIImage
import kotlin.coroutines.resume

/** App-lifetime system integration; never owned by the full-screen composable. */
internal class IosMediaSession(private val coordinator: PlayerCoordinator) : AutoCloseable {
    private val audio = AVAudioSession.sharedInstance()
    private val center = NSNotificationCenter.defaultCenter
    private val remote = MPRemoteCommandCenter.sharedCommandCenter()
    private val nowPlaying = MPNowPlayingInfoCenter.defaultCenter()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val commands = mutableListOf<Pair<MPRemoteCommand, Any>>()
    private val observers = mutableListOf<Any>()
    private val resumePolicy = PlaybackResumePolicy(
        initiallyBackground = UIApplication.sharedApplication.applicationState == UIApplicationStateBackground
    )
    private var artworkUrl: String? = null
    private var artwork: MPMediaItemArtwork? = null
    private var artworkJob: Job? = null

    private var systemCommand = false

    init {
        coordinator.onPlaybackCommand = ::userCommand
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
            if (resumePolicy.enterForeground()) {
                activateAudio(); systemPause(false)
            }
            updateNowPlaying()
        }
        observe(AVAudioSessionInterruptionNotification) { notification ->
            val info = notification.userInfo.orEmpty()
            val type = (info[AVAudioSessionInterruptionTypeKey] as? NSNumber)?.unsignedIntegerValue
            if (type == AVAudioSessionInterruptionTypeBegan) {
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
                    activateAudio(); systemPause(false)
                }
            }
        }
        observe(AVAudioSessionRouteChangeNotification) { notification ->
            if ((notification.userInfo?.get(AVAudioSessionRouteChangeReasonKey) as? NSNumber)?.unsignedIntegerValue == AVAudioSessionRouteChangeReasonOldDeviceUnavailable) {
                send(PlayerCommand.Paused(true))
            }
        }
        observe(AVAudioSessionMediaServicesWereResetNotification) {
            if (!coordinator.playerState.value.paused) activateAudio()
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
                    val local =
                        state.currentMedia?.playlistMediaBean?.takeIf { it.isLocalFile }?.url
                    val url = state.currentMedia?.thumbnail ?: local
                    if (url != artworkUrl) {
                        artworkUrl = url
                        artwork = null
                        artworkJob?.cancel()
                        if (url != null) artworkJob = launch {
                            val image = if (url == local) withContext(Dispatchers.IO) {
                                getLocalMediaThumbnailData(url)?.takeIf { it.isNotEmpty() }
                                    ?.usePinned {
                                        UIImage.imageWithData(
                                            NSData.create(
                                                it.addressOf(0),
                                                it.get().size.toULong()
                                            )
                                        )
                                    }
                            } else loadArtwork(url)
                            if (image != null && artworkUrl == url) {
                                artwork = MPMediaItemArtwork(image.size) { image }
                                updateNowPlaying()
                            }
                        }
                    }
                    updateNowPlaying()
                }
        }
    }

    private suspend fun loadArtwork(url: String): UIImage? =
        suspendCancellableCoroutine { continuation ->
            val address = NSURL.URLWithString(url)
            if (address == null) {
                continuation.resume(null); return@suspendCancellableCoroutine
            }
            val task = NSURLSession.sharedSession.dataTaskWithURL(address) { data, _, _ ->
                continuation.resume(data?.let { UIImage.imageWithData(it) })
            }
            continuation.invokeOnCancellation { task.cancel() }
            task.resume()
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
        val startsPlayback = command is PlayerCommand.LoadList ||
                command is PlayerCommand.PlayFileInPlaylist || command == PlayerCommand.NextMedia ||
                command == PlayerCommand.PreviousMedia || command == PlayerCommand.PlayOrPause ||
                command == PlayerCommand.Paused(false)
        if (command is PlayerCommand.Paused || startsPlayback) {
            val allowsResume = when (command) {
                is PlayerCommand.Paused -> !command.paused
                PlayerCommand.PlayOrPause -> coordinator.playerState.value.paused
                else -> startsPlayback
            }
            resumePolicy.userAction(allowsResume)
            if (startsPlayback) playerTrace("Player/AudioSessionActivate") { activateAudio() }
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

    private fun activateAudio() = memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        if (!audio.setCategory(AVAudioSessionCategoryPlayback, error.ptr) || !audio.setActive(
                true,
                error.ptr
            )
        ) {
            Logger.w("Audio session: ${error.value?.localizedDescription}", tag = "IosMediaSession")
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
        coordinator.onPlaybackCommand = null
        observers.forEach(center::removeObserver)
        commands.forEach { (command, token) ->
            command.removeTarget(token); command.enabled = false
        }
        scope.cancel()
        nowPlaying.nowPlayingInfo = null
        UIApplication.sharedApplication.idleTimerDisabled = false
        audio.setActive(false, AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation, null)
    }
}
