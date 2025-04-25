package com.skyd.anivu.ui.mpv.land.controller

import android.content.Context
import android.media.AudioManager
import android.os.Build
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.window.layout.WindowMetricsCalculator
import com.skyd.anivu.ext.activity
import com.skyd.anivu.ext.detectDoubleFingerTransformGestures
import com.skyd.anivu.ext.getScreenBrightness
import com.skyd.anivu.ext.toRect
import com.skyd.anivu.model.preference.player.PlayerDoubleTapPreference
import com.skyd.anivu.ui.mpv.component.state.PlayState
import com.skyd.anivu.ui.mpv.component.state.PlayStateCallback
import com.skyd.anivu.ui.mpv.land.controller.state.TransformState
import com.skyd.anivu.ui.mpv.land.controller.state.TransformStateCallback
import kotlin.math.abs

private val inSystemBarArea: PointerInputScope.(
    context: Context,
    safeGestures: Rect,
    x: Float,
    y: Float,
) -> Boolean = { context, safeGestures, x, y ->
    val bounds = WindowMetricsCalculator
        .getOrCreate()
        .computeCurrentWindowMetrics(context)
        .bounds
    val width = bounds.width()
    val height = bounds.height()
    val inGesturesArea = x <= safeGestures.left ||
            x >= width - safeGestures.right ||
            y <= safeGestures.top ||
            y >= height - safeGestures.bottom
    inGesturesArea || y / density <= 60 || (width - x) / density <= 60
}

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
    cancelAutoHideControllerRunnable: () -> Boolean,
    restartAutoHideControllerRunnable: () -> Unit,
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
                restartAutoHideControllerRunnable()
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
                cancelAutoHideControllerRunnable()
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
    onVolumeRangeChanged: (IntRange) -> Unit,
    onVolumeChanged: (Int) -> Unit,
    playState: () -> PlayState,
    playStateCallback: PlayStateCallback,
    onShowSeekTimePreview: (Boolean) -> Unit,
    onTimePreviewChanged: (Long) -> Unit,
    transformState: () -> TransformState,
    transformStateCallback: TransformStateCallback,
    cancelAutoHideControllerRunnable: () -> Boolean,
    restartAutoHideControllerRunnable: () -> Unit,
): Modifier {
    if (!enabled()) {
        onShowBrightness(false)
        onShowVolume(false)
        onShowSeekTimePreview(false)
        restartAutoHideControllerRunnable()
        return this
    }

    val currentPlayState by rememberUpdatedState(newValue = playState)

    val safeGestures by rememberUpdatedState(newValue = WindowInsets.safeGestures.toRect())
    var pointerStartX by rememberSaveable { mutableFloatStateOf(0f) }
    var pointerStartY by rememberSaveable { mutableFloatStateOf(0f) }

    val context = LocalContext.current
    val activity = remember(context) { context.activity }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val minVolume = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
    } else 0

    var startBrightness by rememberSaveable { mutableFloatStateOf(0f) }
    var startVolume by rememberSaveable { mutableIntStateOf(0) }

    var seekTimePreviewStartPosition by remember { mutableLongStateOf(0L) }
    var seekTimePreviewPositionDelta by remember { mutableLongStateOf(0L) }

    return pointerInput(Unit) {
        detectDoubleFingerTransformGestures(
            onVerticalDragStart = onVerticalDragStart@{
                cancelAutoHideControllerRunnable()
                pointerStartX = it.x
                pointerStartY = it.y
                if (inSystemBarArea(context, safeGestures, it.x, it.y)) {
                    return@onVerticalDragStart
                }
                when (pointerStartX) {
                    in 0f..controllerWidth() / 3f -> {
                        onBrightnessRangeChanged(0.01f..1f)
                        startBrightness = activity.window.attributes.apply {
                            if (screenBrightness <= 0.00f) {
                                val brightness = activity.getScreenBrightness()
                                if (brightness != null) {
                                    screenBrightness = brightness / 255.0f
                                    activity.window.setAttributes(this)
                                }
                            }
                        }.screenBrightness
                        onBrightnessChanged(startBrightness)
                        onShowBrightness(true)
                    }

                    in controllerWidth() * 2 / 3f..controllerWidth().toFloat() -> {
                        onVolumeRangeChanged(minVolume..maxVolume)
                        startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        onVolumeChanged(startVolume)
                        onShowVolume(true)
                    }
                }
            },
            onVerticalDragEnd = {
                restartAutoHideControllerRunnable()
                onShowBrightness(false)
                onShowVolume(false)
            },
            onVerticalDragCancel = {
                restartAutoHideControllerRunnable()
                onShowBrightness(false)
                onShowVolume(false)
            },
            onVerticalDrag = onVerticalDrag@{ change, _ ->
                val deltaY = change.position.y - pointerStartY
                if (inSystemBarArea(context, safeGestures, pointerStartX, pointerStartY) ||
                    abs(deltaY) < 50
                ) {
                    return@onVerticalDrag
                }
                when (pointerStartX) {
                    in 0f..controllerWidth() / 3f -> {
                        val layoutParams = activity.window.attributes
                        layoutParams.screenBrightness =
                            (startBrightness - deltaY / controllerHeight())
                                .coerceIn(0.01f..1f)
                        activity.window.setAttributes(layoutParams)
                        onBrightnessChanged(layoutParams.screenBrightness)
                    }

                    in controllerWidth() * 2 / 3f..controllerWidth().toFloat() -> {
                        val desiredVolume = (startVolume - deltaY / controllerHeight() *
                                1.2f * (maxVolume - minVolume)).toInt()
                            .coerceIn(minVolume..maxVolume)
                        onVolumeChanged(desiredVolume)
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, desiredVolume, 0)
                    }
                }
            },
            onHorizontalDragStart = onHorizontalDragStart@{
                cancelAutoHideControllerRunnable()
                pointerStartX = it.x
                pointerStartY = it.y
                if (inSystemBarArea(context, safeGestures, it.x, it.y)) {
                    return@onHorizontalDragStart
                }
                seekTimePreviewStartPosition = currentPlayState().position
                seekTimePreviewPositionDelta = 0
                onShowSeekTimePreview(true)
            },
            onHorizontalDragEnd = onHorizontalDragEnd@{
                onShowSeekTimePreview(false)
                restartAutoHideControllerRunnable()
                if (inSystemBarArea(context, safeGestures, pointerStartX, pointerStartY)) {
                    return@onHorizontalDragEnd
                }
                playStateCallback.onSeekTo(seekTimePreviewStartPosition + seekTimePreviewPositionDelta)
            },
            onHorizontalDragCancel = {
                onShowSeekTimePreview(false)
                restartAutoHideControllerRunnable()
            },
            onHorizontalDrag = onHorizontalDrag@{ change, _ ->
                if (inSystemBarArea(context, safeGestures, pointerStartX, pointerStartY)) {
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