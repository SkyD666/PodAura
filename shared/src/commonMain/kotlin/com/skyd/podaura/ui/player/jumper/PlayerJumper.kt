package com.skyd.podaura.ui.player.jumper

import androidx.compose.runtime.Composable

interface PlayerJumper {
    fun jump(mode: PlayDataMode)
}

@Composable
expect fun rememberPlayerJumper(): PlayerJumper