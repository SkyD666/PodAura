package com.skyd.podaura.ui.player.pip

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.skyd.podaura.ui.player.LocalMacosPlayerSession
import kotlinx.cinterop.useContents
import platform.AVKit.AVPictureInPictureController
import platform.Foundation.NSProcessInfo

internal actual val supportPip: Boolean
    get() = NSProcessInfo.processInfo.operatingSystemVersion.useContents { majorVersion >= 12 } &&
            AVPictureInPictureController.isPictureInPictureSupported()

@Composable
internal actual fun rememberOnEnterPip(): OnEnterPip {
    val session = LocalMacosPlayerSession.current
    // Media readiness is a StateFlow; subscribe so the toolbar also updates when it changes.
    val player = session.coordinator
    player?.playerState?.collectAsState()?.value
    player?.engineState?.collectAsState()?.value
    return remember(session) {
        object : OnEnterPip {
            override val canEnter get() = session.pictureInPicture?.canEnter == true
            override val isActive get() = session.pictureInPicture?.active == true
            override fun enter() {
                session.pictureInPicture?.enter()
            }
        }
    }
}
