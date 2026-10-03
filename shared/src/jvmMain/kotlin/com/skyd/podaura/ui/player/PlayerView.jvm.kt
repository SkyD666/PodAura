package com.skyd.podaura.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.PlayStateCallback
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.player.coordinator.PlayerEngineState
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
    // No ON_STOP -> onBack mapping here: on desktop the window lifecycle STOPS when the
    // window is minimized, which must NOT close the player or stop playback. Closing is
    // fully handled by the window's onCloseRequest, the in-player back button, and the
    // mpv Shutdown event (all routed to closePlayer in ui/window/PlayerWindow.kt).
    val engineState by coordinator.engineState.collectAsStateWithLifecycle()
    DesktopPlayerContent(
        engineState = engineState,
        modifier = modifier,
        bootstrapSurface = { surfaceModifier ->
            MPVSurface(player = coordinator.renderPlayer, modifier = surfaceModifier)
        },
        content = commonContent,
    )
}

@Composable
internal fun DesktopPlayerContent(
    engineState: PlayerEngineState,
    modifier: Modifier = Modifier,
    bootstrapSurface: @Composable (Modifier) -> Unit,
    content: @Composable () -> Unit,
) {
    Box(modifier) {
        // FILE_LOADED depends on this surface choosing a live render backend. Mount it
        // behind the loading UI before Ready, including when the media is audio-only.
        if (engineState == PlayerEngineState.AwaitingMedia ||
            engineState == PlayerEngineState.LoadingMedia
        ) {
            bootstrapSurface(Modifier.matchParentSize())
        }
        content()
    }
}
