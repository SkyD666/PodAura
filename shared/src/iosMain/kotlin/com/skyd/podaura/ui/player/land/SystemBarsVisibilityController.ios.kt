package com.skyd.podaura.ui.player.land

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.skyd.podaura.IosPlayerChrome

@Composable
actual fun rememberSystemBarsVisibilityController(): SystemBarsVisibilityController = remember {
    object : SystemBarsVisibilityController {
        override fun show() = IosPlayerChrome.setFullscreen(false)
        override fun hide() = IosPlayerChrome.setFullscreen(true)
    }
}
