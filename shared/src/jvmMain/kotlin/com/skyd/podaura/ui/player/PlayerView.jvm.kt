package com.skyd.podaura.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import com.skyd.podaura.ui.component.OnLifecycleEvent
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.PlayStateCallback
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.player.mpv.MPVPlayer
import com.skyd.podaura.ui.player.service.PlayerState
import org.openani.mediamp.mpv.compose.MpvMediampPlayerSurface

@Composable
actual fun PlatformPlayerView(
    modifier: Modifier,
    onCommand: (PlayerCommand) -> Unit
) {
    MpvMediampPlayerSurface(
        player = MPVPlayer.instance.mpv.player,
        modifier = modifier,
    )
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
    commonContent()

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_STOP -> onBack()
            else -> Unit
        }
    }
}