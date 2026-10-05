package com.skyd.podaura.ui.player.jumper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.skyd.podaura.ui.player.LocalMacosPlayerSession
import com.skyd.podaura.ui.player.PlayerOpenRequest
import io.github.vinceglb.filekit.PlatformFile

@Composable
actual fun rememberPlayerJumper(): PlayerJumper {
    val session = LocalMacosPlayerSession.current
    return remember(session) {
        object : PlayerJumper {
            override fun jump(mode: PlayDataMode) = session.open(PlayerOpenRequest.Media(mode))
            override fun openFiles(files: List<PlatformFile>) =
                session.open(PlayerOpenRequest.Files(files))
        }
    }
}
