package com.skyd.podaura.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.PlayStateCallback
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.player.mpv.MPVSurface
import com.skyd.podaura.ui.player.service.PlayerState

@Composable
actual fun PlatformPlayerView(
    coordinator: PlayerCoordinator,
    modifier: Modifier,
    onCommand: (PlayerCommand) -> Unit
) {
    MPVSurface(
        player = coordinator.renderPlayer,
        modifier = modifier,
    )
}

@Composable
actual fun PlatformContent(
    modifier: Modifier,
    onBack: () -> Unit,
    coordinator: PlayerCoordinator,
    playerObserver: PlayerCoordinator.Observer,
    playerState: PlayerState,
    playState: PlayState,
    playStateCallback: PlayStateCallback,
    commonContent: @Composable (() -> Unit)
) {
    // No ON_STOP -> onBack mapping here: on desktop the window lifecycle STOPS when the
    // window is minimized, which must NOT close the player or stop playback. Closing is
    // fully handled by the window's onCloseRequest, the in-player back button, and the
    // mpv Shutdown event (all routed to closePlayer in ui/window/PlayerWindow.kt).
    commonContent()
}
