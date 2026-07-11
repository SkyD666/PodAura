package com.skyd.podaura.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.PlayStateCallback
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.player.service.PlayerState

@Composable
actual fun PlatformPlayerView(
    modifier: Modifier,
    onCommand: (PlayerCommand) -> Unit
) {
}

@Composable
actual fun PlatformContent(
    modifier: Modifier,
    onBack: () -> Unit,
    service: PlayerCoordinator,
    playerObserver: PlayerCoordinator.Observer,
    playerState: PlayerState,
    playState: PlayState,
    playStateCallback: PlayStateCallback,
    commonContent: @Composable (() -> Unit)
) {
}