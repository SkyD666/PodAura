package com.skyd.podaura.ui.player.land

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberSystemBarsVisibilityController(): SystemBarsVisibilityController {
    return remember {
        object : SystemBarsVisibilityController {
            override fun show() = Unit
            override fun hide() = Unit
        }
    }
}