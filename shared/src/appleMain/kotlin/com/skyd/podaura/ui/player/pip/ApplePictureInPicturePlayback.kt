package com.skyd.podaura.ui.player.pip

import com.skyd.podaura.ui.player.PlayerCommand
import com.skyd.podaura.ui.player.PlayerEvent
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.player.coordinator.isReady
import kotlinx.cinterop.CValue
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readValue
import kotlinx.cinterop.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import platform.AVFoundation.AVSampleBufferDisplayLayer
import platform.AVKit.AVPictureInPictureController
import platform.AVKit.AVPictureInPictureSampleBufferPlaybackDelegateProtocol
import platform.AVKit.invalidatePlaybackState
import platform.CoreFoundation.CFRelease
import platform.CoreMedia.CMClockGetHostTimeClock
import platform.CoreMedia.CMTime
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.CoreMedia.CMTimeRange
import platform.CoreMedia.CMTimeRangeMake
import platform.CoreMedia.CMTimebaseCreateWithSourceClock
import platform.CoreMedia.CMTimebaseRefVar
import platform.CoreMedia.CMTimebaseSetRate
import platform.CoreMedia.CMTimebaseSetTime
import platform.CoreMedia.CMVideoDimensions
import platform.CoreMedia.kCMTimePositiveInfinity
import platform.CoreMedia.kCMTimeRangeInvalid
import platform.darwin.NSObject
import kotlin.time.Duration.Companion.milliseconds

/** Playback timing and AVKit command completion for sample-buffer PiP. */
internal class ApplePictureInPicturePlayback(
    private val coordinator: PlayerCoordinator,
    private val layer: AVSampleBufferDisplayLayer,
) : NSObject(), AVPictureInPictureSampleBufferPlaybackDelegateProtocol {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var skipCompletion: (() -> Unit)? = null
    private var skipTimeout: Job? = null
    private var seekStarted = false
    private val playbackObserver = PlayerCoordinator.Observer { event ->
        when (event) {
            PlayerEvent.Seek -> if (skipCompletion != null) seekStarted = true
            PlayerEvent.PlaybackRestart -> if (seekStarted) finishPendingSkip()
            is PlayerEvent.EndFile, PlayerEvent.Shutdown -> finishPendingSkip()
            else -> Unit
        }
    }
    private val timebase = memScoped {
        val result = alloc<CMTimebaseRefVar>()
        check(CMTimebaseCreateWithSourceClock(null, CMClockGetHostTimeClock(), result.ptr) == 0)
        checkNotNull(result.value)
    }

    init {
        coordinator.addObserver(playbackObserver)
    }

    fun bind(controller: AVPictureInPictureController?) {
        layer.controlTimebase = timebase
        scope.launch {
            combine(coordinator.playerState, coordinator.engineState) { state, _ ->
                CMTimebaseSetTime(timebase, CMTimeMake(state.position, 1))
                CMTimebaseSetRate(timebase, if (state.paused) 0.0 else state.speed.toDouble())
                controller?.requiresLinearPlayback = !state.seekable
                controller?.invalidatePlaybackState()
            }.collect {}
        }
    }

    override fun pictureInPictureController(
        pictureInPictureController: AVPictureInPictureController,
        setPlaying: Boolean
    ) {
        coordinator.onCommand(PlayerCommand.Paused(!setPlaying))
    }

    override fun pictureInPictureControllerIsPlaybackPaused(pictureInPictureController: AVPictureInPictureController) =
        coordinator.playerState.value.paused

    override fun pictureInPictureControllerTimeRangeForPlayback(pictureInPictureController: AVPictureInPictureController): CValue<CMTimeRange> {
        val state = coordinator.playerState.value
        if (!state.mediaStarted) return kCMTimeRangeInvalid.readValue()
        return CMTimeRangeMake(
            CMTimeMake(0, 1),
            if (state.duration > 0 && state.seekable) CMTimeMake(
                state.duration,
                1
            ) else kCMTimePositiveInfinity.readValue()
        )
    }

    override fun pictureInPictureController(
        pictureInPictureController: AVPictureInPictureController,
        didTransitionToRenderSize: CValue<CMVideoDimensions>
    ) = Unit

    override fun pictureInPictureController(
        pictureInPictureController: AVPictureInPictureController,
        skipByInterval: CValue<CMTime>, completionHandler: () -> Unit
    ) {
        val state = coordinator.playerState.value
        if (!state.seekable || !coordinator.engineState.value.isReady) {
            completionHandler(); return
        }
        finishPendingSkip()
        skipCompletion = completionHandler
        seekStarted = false
        skipTimeout = scope.launch { delay(10_000.milliseconds); finishPendingSkip() }
        val target =
            (state.position + CMTimeGetSeconds(skipByInterval).toLong()).coerceAtLeast(0).let {
                if (state.duration > 0) it.coerceAtMost(state.duration) else it
            }
        coordinator.onCommand(PlayerCommand.SeekTo(target))
    }

    fun finishPendingSkip() {
        val completion = skipCompletion ?: return
        skipCompletion = null
        seekStarted = false
        skipTimeout?.cancel()
        skipTimeout = null
        val state = coordinator.playerState.value
        val position =
            if (coordinator.engineState.value.isReady) coordinator.renderPlayer.getPropertyDouble("time-pos")
            else state.position.toDouble()
        CMTimebaseSetTime(timebase, CMTimeMakeWithSeconds(position, 600))
        CMTimebaseSetRate(timebase, if (state.paused) 0.0 else state.speed.toDouble())
        completion()
    }

    fun close() {
        finishPendingSkip()
        coordinator.removeObserver(playbackObserver)
        scope.cancel()
        layer.controlTimebase = null
        CFRelease(timebase)
    }
}
