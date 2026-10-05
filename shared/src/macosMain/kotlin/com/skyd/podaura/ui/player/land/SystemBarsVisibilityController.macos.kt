package com.skyd.podaura.ui.player.land

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberSystemBarsVisibilityController(): SystemBarsVisibilityController {
    return remember {
        object : SystemBarsVisibilityController {
            // AppKit owns menu bar and Dock visibility in native fullscreen.
            override fun show() = Unit
            override fun hide() = Unit
        }
    }
}
