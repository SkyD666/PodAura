package com.skyd.podaura.ui.player.land

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.skyd.podaura.ui.component.rememberSystemUiController
import com.skyd.podaura.ui.player.PlayerCommand
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.PlayStateCallback
import com.skyd.podaura.ui.player.component.state.dialog.DialogState
import com.skyd.podaura.ui.player.component.state.dialog.OnDialogVisibilityChanged
import com.skyd.podaura.ui.player.land.controller.PlayerController
import com.skyd.podaura.ui.player.land.controller.state.TransformState
import com.skyd.podaura.ui.player.land.controller.state.TransformStateCallback
import java.io.File

@Composable
internal fun FullscreenPlayerView(
    playState: PlayState,
    playStateCallback: PlayStateCallback,
    dialogState: DialogState,
    onDialogVisibilityChanged: OnDialogVisibilityChanged,
    onSaveScreenshot: (File) -> Unit,
    onCommand: (PlayerCommand) -> Unit,
    onExitFullscreen: () -> Unit,
    playerContent: @Composable () -> Unit,
) {
    val systemUiController = rememberSystemUiController()
    var transformState by remember { mutableStateOf(TransformState.initial) }
    val transformStateCallback = remember {
        TransformStateCallback(
            onVideoRotate = { onCommand(PlayerCommand.Rotate(it.toInt())) },
            onVideoZoom = { onCommand(PlayerCommand.Zoom(it)) },
            onVideoOffset = { onCommand(PlayerCommand.VideoOffset(it)) },
        )
    }

    LaunchedEffect(playState) {
        transformState = transformState.copyIfNecessary(
            videoRotate = playState.rotate,
            videoOffset = Offset(x = playState.offsetX, y = playState.offsetY),
            videoZoom = playState.zoom,
        )
    }

    playerContent()
    PlayerController(
        enabled = { playState.mediaLoaded },
        playState = { playState },
        playStateCallback = playStateCallback,
        dialogState = dialogState,
        onDialogVisibilityChanged = onDialogVisibilityChanged,
        transformState = { transformState },
        transformStateCallback = transformStateCallback,
        onScreenshot = { onCommand(PlayerCommand.Screenshot(onSaveScreenshot)) },
        onExitFullscreen = onExitFullscreen,
    )

    LifecycleResumeEffect(Unit) {
        with(systemUiController) {
            isSystemBarsVisible = false
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onPauseOrDispose {
            with(systemUiController) {
                isSystemBarsVisible = true
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            }
        }
    }
}