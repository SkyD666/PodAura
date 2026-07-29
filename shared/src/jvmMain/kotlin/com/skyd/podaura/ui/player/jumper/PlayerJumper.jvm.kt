package com.skyd.podaura.ui.player.jumper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.skyd.podaura.ui.window.LocalDesktopAppState

@Composable
actual fun rememberPlayerJumper(): PlayerJumper {
    val appState = LocalDesktopAppState.current
    return remember(appState) {
        object : PlayerJumper {
            override fun jump(mode: PlayDataMode) = appState.openPlayer(mode)
        }
    }
}
