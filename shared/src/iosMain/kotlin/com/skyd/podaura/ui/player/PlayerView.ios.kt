package com.skyd.podaura.ui.player

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.PlayStateCallback
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.player.service.PlayerState
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIView

private class IosVideoView(private val pip: com.skyd.podaura.ui.player.pip.IosPictureInPicture) :
    UIView(CGRectZero.readValue()) {
    init {
        pip.mount(this)
    }

    override fun layoutSubviews() {
        super.layoutSubviews(); pip.layout(this)
    }

    fun release() {
        pip.unmount(this)
    }
}

@Composable
actual fun PlatformPlayerView(
    coordinator: PlayerCoordinator,
    modifier: Modifier,
    onCommand: (PlayerCommand) -> Unit
) {
    val pip = LocalIosPlayerSession.current.pictureInPicture ?: return
    val focus = remember { FocusRequester() }
    LaunchedEffect(focus) { focus.requestFocus() }
    UIKitView(
        factory = { IosVideoView(pip) },
        modifier = modifier.onPreviewKeyEvent(coordinator::onKey).focusRequester(focus).focusable(),
        onRelease = { it.release() },
        properties = UIKitInteropProperties(
            interactionMode = null,
            isNativeAccessibilityEnabled = false
        ),
    )
}

// System audio and app lifecycle outlive the visible player and belong to IosMediaSession.
@Composable
actual fun PlatformPlayerLifecycleEffect(coordinator: PlayerCoordinator) = Unit

@Composable
actual fun PlatformContent(
    modifier: Modifier, onBack: () -> Unit, coordinator: PlayerCoordinator,
    playerState: PlayerState, playState: PlayState, playStateCallback: PlayStateCallback,
    commonContent: @Composable () -> Unit
) = commonContent()
