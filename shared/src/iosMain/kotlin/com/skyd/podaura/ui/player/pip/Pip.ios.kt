package com.skyd.podaura.ui.player.pip

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.skyd.podaura.ui.player.LocalIosPlayerSession
import platform.AVKit.AVPictureInPictureController

internal actual val supportPip: Boolean get() = AVPictureInPictureController.isPictureInPictureSupported()

@Composable
internal actual fun rememberOnEnterPip(): OnEnterPip {
    val session = LocalIosPlayerSession.current
    return remember(session) {
        object : OnEnterPip {
            override val canEnter get() = session.pictureInPicture?.possible == true
            override fun enter() {
                session.pictureInPicture?.enter()
            }
        }
    }
}
