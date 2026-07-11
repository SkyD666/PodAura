package com.skyd.podaura.ui.player.jumper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.skyd.podaura.ui.window.openPlayerWindow

// The player window itself lives at application scope (see ui/window/PlayerWindow.kt), so
// it survives the disposal of whichever screen this jumper was composed in.
@Composable
actual fun rememberPlayerJumper(): PlayerJumper = remember {
    object : PlayerJumper {
        override fun jump(mode: PlayDataMode) = openPlayerWindow(mode)
    }
}
