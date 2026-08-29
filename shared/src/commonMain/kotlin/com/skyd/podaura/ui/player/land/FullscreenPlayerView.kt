package com.skyd.podaura.ui.player.land

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.skyd.podaura.ui.player.PlayerCommand
import com.skyd.podaura.ui.player.collectPlayerTransform
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.PlayStateCallback
import com.skyd.podaura.ui.player.component.state.dialog.DialogState
import com.skyd.podaura.ui.player.component.state.dialog.OnDialogVisibilityChanged
import com.skyd.podaura.ui.player.land.controller.PlayerController
import com.skyd.podaura.ui.player.land.controller.state.TransformState
import com.skyd.podaura.ui.player.land.controller.state.TransformStateCallback
import com.skyd.podaura.ui.player.service.PlayerState
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun FullscreenPlayerView(
    playerStateFlow: StateFlow<PlayerState>,
    playState: PlayState,
    playStateCallback: PlayStateCallback,
    dialogState: DialogState,
    onDialogVisibilityChanged: OnDialogVisibilityChanged,
    onSaveScreenshot: (PlatformFile) -> Unit,
    onCommand: (PlayerCommand) -> Unit,
    snackbarHostState: SnackbarHostState,
    onExitFullscreen: () -> Unit,
    playerContent: @Composable () -> Unit,
) {
    val playerTransform by collectPlayerTransform(playerStateFlow)
    var transformState by remember { mutableStateOf(TransformState.initial) }
    val transformStateCallback = remember {
        TransformStateCallback(
            onVideoRotate = { onCommand(PlayerCommand.Rotate(it.toInt())) },
            onVideoZoom = { onCommand(PlayerCommand.Zoom(it)) },
            onVideoOffset = { onCommand(PlayerCommand.VideoOffset(it)) },
        )
    }

    LaunchedEffect(playerTransform) {
        transformState = transformState.copyIfNecessary(
            videoRotate = playerTransform.rotate,
            videoOffset = Offset(x = playerTransform.offsetX, y = playerTransform.offsetY),
            videoZoom = playerTransform.zoom,
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        playerContent()
        PlayerController(
            playerStateFlow = playerStateFlow,
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
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    val systemBarsVisibilityController = rememberSystemBarsVisibilityController()
    LifecycleResumeEffect(Unit) {
        systemBarsVisibilityController.hide()
        onPauseOrDispose { systemBarsVisibilityController.show() }
    }
}
