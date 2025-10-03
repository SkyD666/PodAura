package com.skyd.podaura.ui.player.land.controller

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import com.skyd.podaura.ext.detectDoubleFingerTransformGestures
import com.skyd.podaura.model.preference.player.PlayerDoubleTapPreference
import com.skyd.podaura.ui.component.rememberAudioController
import com.skyd.podaura.ui.component.rememberBrightnessController
import com.skyd.podaura.ui.component.rememberSystemBarAreaDetector
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.PlayStateCallback
import com.skyd.podaura.ui.player.land.controller.state.TransformState
import com.skyd.podaura.ui.player.land.controller.state.TransformStateCallback
import kotlin.math.abs

@Composable
internal fun Modifier.detectPressGestures(
    controllerWidth: () -> Int,
    playState: () -> PlayState,
    playStateCallback: PlayStateCallback,
    showController: () -> Boolean,
    onShowControllerChanged: (Boolean) -> Unit,
    isLongPressing: () -> Boolean,
    isLongPressingChanged: (Boolean) -> Unit,
    onShowForwardRipple: (Offset) -> Unit,
    onShowBackwardRipple: (Offset) -> Unit,
    cancelAutoHideController: () -> Unit,
    restartAutoHideController: () -> Unit,
): Modifier {
    var beforeLongPressingSpeed by rememberSaveable { mutableFloatStateOf(playState().speed) }

    val playerDoubleTap = PlayerDoubleTapPreference.current
    val onDoubleTapPausePlay: () -> Unit = remember { { playStateCallback.onPlayOrPause() } }

    val onDoubleTapBackwardForward: PlayState.(Offset) -> Unit = { offset ->
        if (offset.x < controllerWidth() / 2f) {
            playStateCallback.onSeekTo(position - 10) // -10s.
            onShowBackwardRipple(offset)
        } else {
            playStateCallback.onSeekTo(position + 10) // +10s.
            onShowForwardRipple(offset)
        }
    }
    val onDoubleTapBackwardPausePlayForward: PlayState.(Offset) -> Unit = { offset ->
        if (offset.x <= controllerWidth() * 0.25f) {
            playStateCallback.onSeekTo(position - 10) // -10s.
            onShowBackwardRipple(offset)
        } else if (offset.x >= controllerWidth() * 0.75f) {
            playStateCallback.onSeekTo(position + 10) // +10s.
            onShowForwardRipple(offset)
        } else {
            onDoubleTapPausePlay()
        }
    }

    val onDoubleTap: (Offset) -> Unit = { offset ->
        when (playerDoubleTap) {
            PlayerDoubleTapPreference.BACKWARD_FORWARD ->
                playState().onDoubleTapBackwardForward(offset)

            PlayerDoubleTapPreference.BACKWARD_PAUSE_PLAY_FORWARD ->
                playState().onDoubleTapBackwardPausePlayForward(offset)

            else -> onDoubleTapPausePlay()
        }
    }

    return pointerInput(playerDoubleTap) {
        detectTapGestures(
            onLongPress = {
                beforeLongPressingSpeed = playState().speed
                isLongPressingChanged(true)
                playStateCallback.onSpeedChanged(3f)
            },
            onDoubleTap = {
                restartAutoHideController()
                onDoubleTap(it)
            },
            onPress = {
                tryAwaitRelease()
                if (isLongPressing()) {
                    isLongPressingChanged(false)
                    playStateCallback.onSpeedChanged(beforeLongPressingSpeed)
                }
            },
            onTap = {
                cancelAutoHideController()
                onShowControllerChanged(!showController())
            }
        )
    }
}

@Composable
internal fun Modifier.detectControllerGestures(
    enabled: () -> Boolean,
    controllerWidth: () -> Int,
    controllerHeight: () -> Int,
    onShowBrightness: (Boolean) -> Unit,
    onBrightnessRangeChanged: (ClosedFloatingPointRange<Float>) -> Unit,
    onBrightnessChanged: (Float) -> Unit,
    onShowVolume: (Boolean) -> Unit,
    onVolumeRangeChanged: (ClosedFloatingPointRange<Float>) -> Unit,
    onVolumeChanged: (Float) -> Unit,
    playState: () -> PlayState,
    playStateCallback: PlayStateCallback,
    onShowSeekTimePreview: (Boolean) -> Unit,
    onTimePreviewChanged: (Long) -> Unit,
    transformState: () -> TransformState,
    transformStateCallback: TransformStateCallback,
    cancelAutoHideController: () -> Unit,
    restartAutoHideController: () -> Unit,
): Modifier {
    if (!enabled()) {
        onShowBrightness(false)
        onShowVolume(false)
        onShowSeekTimePreview(false)
        restartAutoHideController()
        return this
    }

    val currentPlayState by rememberUpdatedState(newValue = playState)

    var pointerStartX by rememberSaveable { mutableFloatStateOf(0f) }
    var pointerStartY by rememberSaveable { mutableFloatStateOf(0f) }

    var startBrightness by rememberSaveable { mutableFloatStateOf(0f) }
    var startVolume by rememberSaveable { mutableFloatStateOf(0f) }

    var seekTimePreviewStartPosition by remember { mutableLongStateOf(0L) }
    var seekTimePreviewPositionDelta by remember { mutableLongStateOf(0L) }

    val brightnessController = rememberBrightnessController()
    val audioController = rememberAudioController()
    val systemBarAreaDetector = rememberSystemBarAreaDetector()

    return pointerInput(Unit) {
        detectDoubleFingerTransformGestures(
            onVerticalDragStart = onVerticalDragStart@{
                cancelAutoHideController()
                pointerStartX = it.x
                pointerStartY = it.y
                if (systemBarAreaDetector.inSystemBarArea(it.x, it.y)) {
                    return@onVerticalDragStart
                }
                when (pointerStartX) {
                    in 0f..controllerWidth() / 3f -> {
                        onBrightnessRangeChanged(0.01f..1f)
                        startBrightness = brightnessController.percent
                        onBrightnessChanged(startBrightness)
                        onShowBrightness(true)
                    }

                    in controllerWidth() * 2 / 3f..controllerWidth().toFloat() -> {
                        onVolumeRangeChanged(audioController.range)
                        startVolume = audioController.value
                        onVolumeChanged(startVolume)
                        onShowVolume(true)
                    }
                }
            },
            onVerticalDragEnd = {
                restartAutoHideController()
                onShowBrightness(false)
                onShowVolume(false)
            },
            onVerticalDragCancel = {
                restartAutoHideController()
                onShowBrightness(false)
                onShowVolume(false)
            },
            onVerticalDrag = onVerticalDrag@{ change, _ ->
                val deltaY = change.position.y - pointerStartY
                if (systemBarAreaDetector.inSystemBarArea(pointerStartX, pointerStartY) ||
                    abs(deltaY) < 50
                ) {
                    return@onVerticalDrag
                }
                when (pointerStartX) {
                    in 0f..controllerWidth() / 3f -> {
                        val newScreenBrightness = startBrightness - deltaY / controllerHeight()
                        brightnessController.percent = newScreenBrightness
                        onBrightnessChanged(newScreenBrightness)
                    }

                    in controllerWidth() * 2 / 3f..controllerWidth().toFloat() -> {
                        val newVolume = startVolume - deltaY / controllerHeight() * 1.2f *
                                audioController.range.run { endInclusive - start }
                        audioController.value = newVolume
                        onVolumeChanged(newVolume)

                    }
                }
            },
            onHorizontalDragStart = onHorizontalDragStart@{
                cancelAutoHideController()
                pointerStartX = it.x
                pointerStartY = it.y
                if (systemBarAreaDetector.inSystemBarArea(it.x, it.y)) {
                    return@onHorizontalDragStart
                }
                seekTimePreviewStartPosition = currentPlayState().position
                seekTimePreviewPositionDelta = 0
                onShowSeekTimePreview(true)
            },
            onHorizontalDragEnd = onHorizontalDragEnd@{
                onShowSeekTimePreview(false)
                restartAutoHideController()
                if (systemBarAreaDetector.inSystemBarArea(pointerStartX, pointerStartY)) {
                    return@onHorizontalDragEnd
                }
                playStateCallback.onSeekTo(seekTimePreviewStartPosition + seekTimePreviewPositionDelta)
            },
            onHorizontalDragCancel = {
                onShowSeekTimePreview(false)
                restartAutoHideController()
            },
            onHorizontalDrag = onHorizontalDrag@{ change, _ ->
                if (systemBarAreaDetector.inSystemBarArea(pointerStartX, pointerStartY)) {
                    return@onHorizontalDrag
                }
                seekTimePreviewPositionDelta =
                    ((change.position.x - pointerStartX) / density / 8).toLong()
                onTimePreviewChanged(seekTimePreviewStartPosition + seekTimePreviewPositionDelta)
            },
            onGesture = onGesture@{ _: Offset, pan: Offset, zoom: Float, rotation: Float ->
                with(transformState()) {
                    transformStateCallback.onVideoOffset(videoOffset + pan / videoZoom)
                    transformStateCallback.onVideoRotate(videoRotate + rotation)
                    transformStateCallback.onVideoZoom(videoZoom * zoom)
                }
            }
        )
    }
}