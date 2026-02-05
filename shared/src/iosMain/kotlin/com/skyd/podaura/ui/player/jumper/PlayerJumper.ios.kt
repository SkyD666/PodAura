package com.skyd.podaura.ui.player.jumper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberPlayerJumper(): PlayerJumper {
    return remember {
        object : PlayerJumper {
            override fun jump(mode: PlayDataMode) {

            }
        }
    }
}
