package com.skyd.podaura.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skyd.podaura.ui.component.AppKitView
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.PlayStateCallback
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.player.pip.MacosPictureInPicture
import com.skyd.podaura.ui.player.service.PlayerState
import kotlinx.cinterop.CValue
import org.jetbrains.compose.resources.stringResource
import platform.AppKit.NSView
import platform.CoreGraphics.CGRect
import platform.Foundation.NSMakeRect
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.player_pip_active
import podaura.shared.generated.resources.player_pip_failed
import podaura.shared.generated.resources.player_pip_restore

private class MacosVideoView(private val pip: MacosPictureInPicture) :
    NSView(NSMakeRect(0.0, 0.0, 1.0, 1.0)) {
    init {
        pip.mount(this)
    }

    override fun setFrame(frame: CValue<CGRect>) {
        super.setFrame(frame)
        pip.layout(this)
    }

    override fun viewDidChangeBackingProperties() {
        super.viewDidChangeBackingProperties()
        pip.layout(this)
    }
}

@Composable
actual fun PlatformPlayerView(
    coordinator: PlayerCoordinator,
    modifier: Modifier,
    onCommand: (PlayerCommand) -> Unit,
) {
    val pip = LocalMacosPlayerSession.current.pictureInPicture ?: return
    key(coordinator) {
        Box(modifier) {
            MacosVideoSurface(Modifier.fillMaxSize(), pip)
            if (pip.active) {
                Column(
                    modifier = Modifier.fillMaxSize().background(Color.Black).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                ) {
                    Text(
                        stringResource(Res.string.player_pip_active), color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = pip::restore) {
                        Text(stringResource(Res.string.player_pip_restore))
                    }
                }
            }
        }
    }
}

@Composable
internal fun MacosVideoSurface(modifier: Modifier, pip: MacosPictureInPicture) {
    AppKitView(
        factory = { MacosVideoView(pip) },
        modifier = modifier,
        onRelease = pip::unmount,
    )
}

// A minimized desktop window keeps playing, regardless of the background preference.
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
    commonContent: @Composable () -> Unit,
) {
    val pip = LocalMacosPlayerSession.current.pictureInPicture
    val snackbar = remember { SnackbarHostState() }
    val error = pip?.error
    val message = stringResource(Res.string.player_pip_failed, error.orEmpty())
    LaunchedEffect(error) {
        if (error != null) {
            snackbar.showSnackbar(message)
            if (pip.error == error) pip.error = null
        }
    }
    Box(modifier) {
        commonContent()
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}
