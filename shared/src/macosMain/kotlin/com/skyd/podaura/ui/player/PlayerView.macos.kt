package com.skyd.podaura.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.PlayStateCallback
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.player.service.PlayerState

@Composable
actual fun PlatformPlayerView(
    coordinator: PlayerCoordinator,
    modifier: Modifier,
    onCommand: (PlayerCommand) -> Unit
) {
}

@Composable
actual fun PlatformPlayerLifecycleEffect(coordinator: PlayerCoordinator) = Unit

@Composable
actual fun PlatformContent(
    modifier: Modifier,
    onBack: () -> Unit,
    coordinator: PlayerCoordinator,
    playerState: PlayerState,
    playState: PlayState,
    playStateCallback: PlayStateCallback,
    commonContent: @Composable (() -> Unit)
) {
}
