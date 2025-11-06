package com.skyd.podaura.ui.player.land.controller

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.skyd.podaura.model.preference.player.PlayerForwardSecondsPreference
import com.skyd.podaura.model.preference.player.PlayerReplaySecondsPreference
import com.skyd.podaura.model.preference.player.PlayerShowForwardSecondsButtonPreference
import com.skyd.podaura.model.preference.player.PlayerShowProgressIndicatorPreference
import com.skyd.podaura.model.preference.player.PlayerShowReplaySecondsButtonPreference
import com.skyd.podaura.model.preference.player.PlayerShowScreenshotButtonPreference
import com.skyd.podaura.ui.component.shape.ForwardRippleDirect
import com.skyd.podaura.ui.component.tickVibrate
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.PlayStateCallback
import com.skyd.podaura.ui.player.component.state.dialog.DialogState
import com.skyd.podaura.ui.player.component.state.dialog.OnDialogVisibilityChanged
import com.skyd.podaura.ui.player.land.controller.bar.BottomBar
import com.skyd.podaura.ui.player.land.controller.bar.TopBar
import com.skyd.podaura.ui.player.land.controller.button.ForwardOrReplaySeconds
import com.skyd.podaura.ui.player.land.controller.button.ResetTransform
import com.skyd.podaura.ui.player.land.controller.button.Screenshot
import com.skyd.podaura.ui.player.land.controller.preview.BrightnessPreview
import com.skyd.podaura.ui.player.land.controller.preview.LongPressSpeedPreview
import com.skyd.podaura.ui.player.land.controller.preview.SeekTimePreview
import com.skyd.podaura.ui.player.land.controller.preview.VolumePreview
import com.skyd.podaura.ui.player.land.controller.state.TransformState
import com.skyd.podaura.ui.player.land.controller.state.TransformStateCallback
import com.skyd.podaura.ui.player.land.rememberSystemBarsVisibilityController
import com.skyd.podaura.ui.screen.playlist.medialist.list.PlaylistMediaList
import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.platform
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tech.annexflow.constraintlayout.compose.ConstraintLayout


@Composable
        /*internal*/ fun PlayerController(
    enabled: () -> Boolean,
    playState: () -> PlayState,
    playStateCallback: PlayStateCallback,
    dialogState: DialogState,
    onDialogVisibilityChanged: OnDialogVisibilityChanged,
    transformState: () -> TransformState,
    transformStateCallback: TransformStateCallback,
    onScreenshot: () -> Unit,
    onExitFullscreen: () -> Unit,
) {
    var showController by rememberSaveable { mutableStateOf(true) }
    var controllerWidth by remember { mutableIntStateOf(0) }
    var controllerHeight by remember { mutableIntStateOf(0) }
    var controllerLayoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val scope = rememberCoroutineScope()

    var autoHideControllerJob: Job? = null
    fun cancelAutoHideController() {
        autoHideControllerJob?.cancel()
    }

    fun restartAutoHideController() {
        cancelAutoHideController()
        if (showController) {
            autoHideControllerJob = scope.launch {
                delay(5000)
                if (isActive) {
                    showController = false
                }
            }
        }
    }
    LaunchedEffect(showController) { restartAutoHideController() }

    var showSeekTimePreview by remember { mutableStateOf(false) }
    var seekTimePreview by remember { mutableLongStateOf(0L) }

    var showBrightnessPreview by remember { mutableStateOf(false) }
    var brightnessValue by remember { mutableFloatStateOf(0f) }
    var brightnessRange by remember { mutableStateOf(0f..0f) }

    var showVolumePreview by remember { mutableStateOf(false) }
    var volumeValue by remember { mutableFloatStateOf(0f) }
    var volumeRange by remember { mutableStateOf(0f..0f) }

    var showForwardRipple by remember { mutableStateOf(false) }
    var forwardRippleStartControllerOffset by remember { mutableStateOf(Offset.Zero) }
    var showBackwardRipple by remember { mutableStateOf(false) }
    var backwardRippleStartControllerOffset by remember { mutableStateOf(Offset.Zero) }

    var isLongPressing by remember { mutableStateOf(false) }

    var showPlaylistSheet by remember { mutableStateOf(false) }
    val playlistSheetState = rememberModalBottomSheetState()

    LaunchedEffect(dialogState.subtitleTrackDialogState()) {
        if (dialogState.subtitleTrackDialogState().show) cancelAutoHideController()
        else restartAutoHideController()
    }

    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    controllerWidth = it.size.width
                    controllerHeight = it.size.height
                    controllerLayoutCoordinates = it
                }
                // detectControllerGestures should be called before detectPressGestures
                // to avoid responding to swipes when long pressing
                .detectControllerGestures(
                    enabled = enabled,
                    controllerWidth = { controllerWidth },
                    controllerHeight = { controllerHeight },
                    onShowBrightness = { showBrightnessPreview = it },
                    onBrightnessRangeChanged = { brightnessRange = it },
                    onBrightnessChanged = { brightnessValue = it },
                    onShowVolume = { showVolumePreview = it },
                    onVolumeRangeChanged = { volumeRange = it },
                    onVolumeChanged = { volumeValue = it },
                    playState = playState,
                    playStateCallback = playStateCallback,
                    onShowSeekTimePreview = { showSeekTimePreview = it },
                    onTimePreviewChanged = { seekTimePreview = it },
                    transformState = transformState,
                    transformStateCallback = transformStateCallback,
                    cancelAutoHideController = ::cancelAutoHideController,
                    restartAutoHideController = ::restartAutoHideController,
                )
                .detectPressGestures(
                    controllerWidth = { controllerWidth },
                    playState = playState,
                    playStateCallback = playStateCallback,
                    showController = { showController },
                    onShowControllerChanged = { showController = it },
                    isLongPressing = { isLongPressing },
                    isLongPressingChanged = { isLongPressing = it },
                    onShowForwardRipple = {
                        forwardRippleStartControllerOffset = it
                        showForwardRipple = true
                    },
                    onShowBackwardRipple = {
                        backwardRippleStartControllerOffset = it
                        showBackwardRipple = true
                    },
                    cancelAutoHideController = ::cancelAutoHideController,
                    restartAutoHideController = ::restartAutoHideController,
                )
        ) {
            // Forward ripple
            AnimatedVisibility(
                visible = showForwardRipple,
                modifier = Modifier.align(Alignment.CenterEnd),
                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessHigh)),
                exit = fadeOut(),
            ) {
                ForwardRipple(
                    direct = ForwardRippleDirect.Forward,
                    text = "+10s",
                    icon = Icons.Rounded.FastForward,
                    controllerWidth = { controllerWidth },
                    parentLayoutCoordinates = controllerLayoutCoordinates,
                    rippleStartControllerOffset = forwardRippleStartControllerOffset,
                    onHideRipple = { showForwardRipple = false },
                )
            }
            // Backward ripple
            AnimatedVisibility(
                visible = showBackwardRipple,
                modifier = Modifier.align(Alignment.CenterStart),
                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessHigh)),
                exit = fadeOut(),
            ) {
                ForwardRipple(
                    direct = ForwardRippleDirect.Backward,
                    text = "-10s",
                    icon = Icons.Rounded.FastRewind,
                    controllerWidth = { controllerWidth },
                    parentLayoutCoordinates = controllerLayoutCoordinates,
                    rippleStartControllerOffset = backwardRippleStartControllerOffset,
                    onHideRipple = { showBackwardRipple = false },
                )
            }
            // Auto hide box
            AutoHiddenBox(
                enabled = enabled,
                show = { showController },
                playState = playState,
                playStateCallback = playStateCallback,
                onDialogVisibilityChanged = onDialogVisibilityChanged,
                transformState = transformState,
                transformStateCallback = transformStateCallback,
                onScreenshot = onScreenshot,
                onRestartAutoHideController = ::restartAutoHideController,
                onOpenPlaylist = { showPlaylistSheet = true },
                onExitFullscreen = onExitFullscreen,
            )

            if (playState().loading) {
                LoadingIndicator(modifier = Modifier.align(Alignment.Center))
            }

            if (PlayerShowProgressIndicatorPreference.current && !showController) {
                ProgressIndicator(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    playState = playState,
                )
            }

            // Seek time preview
            if (showSeekTimePreview) {
                SeekTimePreview(
                    value = { seekTimePreview },
                    duration = { playState().duration },
                )
            }
            // Brightness preview
            if (showBrightnessPreview) {
                BrightnessPreview(value = { brightnessValue }, range = { brightnessRange })
            }
            // Volume preview
            if (showVolumePreview) {
                VolumePreview(value = { volumeValue }, range = { volumeRange })
            }
            // Long press speed preview
            if (isLongPressing) {
                LaunchedEffect(Unit) {
                    if (isLongPressing) {
                        tickVibrate()
                    }
                }
                LongPressSpeedPreview(speed = { playState().speed })
            }
        }
    }

    if (showPlaylistSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPlaylistSheet = false },
            sheetState = playlistSheetState
        ) {
            val playStateValue = playState()
            PlaylistMediaList(
                currentPlaylistId = playStateValue.playlistId,
                currentPlay = playStateValue.currentMedia,
                playlist = remember(playStateValue) { playStateValue.playlist.values.toList() },
                onPlay = { playStateCallback.onPlayFileInPlaylist(it.playlistMediaBean.url) },
                onDelete = { playStateCallback.onRemoveFromPlaylist(it) },
            )
        }
    }

    if (platform == Platform.Android) {
        val systemBarsVisibilityController = rememberSystemBarsVisibilityController()
        LaunchedEffect(
            dialogState.speedDialogState().show,
            dialogState.subtitleTrackDialogState().show,
            dialogState.audioTrackDialogState().show,
            showPlaylistSheet,
        ) {
            delay(200)
            systemBarsVisibilityController.hide()
        }
    }
}

@Composable
private fun AutoHiddenBox(
    enabled: () -> Boolean,
    show: () -> Boolean,
    playState: () -> PlayState,
    playStateCallback: PlayStateCallback,
    onDialogVisibilityChanged: OnDialogVisibilityChanged,
    transformState: () -> TransformState,
    transformStateCallback: TransformStateCallback,
    onScreenshot: () -> Unit,
    onRestartAutoHideController: () -> Unit,
    onOpenPlaylist: () -> Unit,
    onExitFullscreen: () -> Unit,
) {
    Box {
        AnimatedVisibility(
            visible = show(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            ConstraintLayout(modifier = Modifier.fillMaxSize()) {
                val (topBar, bottomBar, screenshot, replaySeconds, forwardSeconds, resetTransform) = createRefs()

                TopBar(
                    modifier = Modifier.constrainAs(topBar) { top.linkTo(parent.top) },
                    title = playState().run { title.orEmpty().ifBlank { mediaTitle }.orEmpty() },
                    onExitFullscreen = onExitFullscreen,
                )
                BottomBar(
                    modifier = Modifier.constrainAs(bottomBar) { bottom.linkTo(parent.bottom) },
                    enabled = enabled,
                    playStateCallback = playStateCallback,
                    playState = playState,
                    onDialogVisibilityChanged = onDialogVisibilityChanged,
                    onRestartAutoHideController = onRestartAutoHideController,
                    onOpenPlaylist = onOpenPlaylist,
                    onExitFullscreen = onExitFullscreen,
                )

                if (PlayerShowScreenshotButtonPreference.current) {
                    Screenshot(
                        modifier = Modifier
                            .constrainAs(screenshot) {
                                bottom.linkTo(parent.bottom)
                                top.linkTo(parent.top)
                                end.linkTo(parent.end)
                            }
                            .padding(end = 20.dp),
                        onClick = onScreenshot,
                    )
                }

                // Replay seconds button
                if (PlayerShowReplaySecondsButtonPreference.current) {
                    val replaySecond = PlayerReplaySecondsPreference.current
                    ForwardOrReplaySeconds(
                        modifier = Modifier
                            .constrainAs(replaySeconds) {
                                bottom.linkTo(bottomBar.top)
                                start.linkTo(parent.start)
                            }
                            .padding(start = 20.dp)
                            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Start)),
                        seconds = replaySecond,
                        onLongClick = { onDialogVisibilityChanged.onReplaySecondDialog(true) },
                        onClick = {
                            with(playState()) { playStateCallback.onSeekTo(position + replaySecond) }
                            onRestartAutoHideController()
                        },
                    )
                }

                // Forward seconds button
                if (PlayerShowForwardSecondsButtonPreference.current) {
                    val forwardSecond = PlayerForwardSecondsPreference.current
                    ForwardOrReplaySeconds(
                        modifier = Modifier
                            .constrainAs(forwardSeconds) {
                                bottom.linkTo(bottomBar.top)
                                end.linkTo(parent.end)
                            }
                            .padding(end = 20.dp),
                        seconds = forwardSecond,
                        onLongClick = { onDialogVisibilityChanged.onForwardSecondDialog(true) },
                        onClick = {
                            with(playState()) { playStateCallback.onSeekTo(position + forwardSecond) }
                            onRestartAutoHideController()
                        },
                    )
                }

                // Reset transform
                if (transformState().run {
                        videoZoom != 1f || videoRotate != 0f || videoOffset != Offset.Zero
                    }
                ) {
                    ResetTransform(
                        modifier = Modifier.constrainAs(resetTransform) {
                            bottom.linkTo(bottomBar.top)
                            top.linkTo(parent.top)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            verticalBias = 1f
                        },
                        enabled = enabled,
                        onClick = {
                            with(transformStateCallback) {
                                onVideoOffset(Offset.Zero)
                                onVideoZoom(1f)
                                onVideoRotate(0f)
                            }
                        }
                    )
                }
            }
        }
    }
}