package com.skyd.podaura.ui.player.land

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.skyd.fundation.util.notSupport

@Composable
actual fun rememberSystemBarsVisibilityController(): SystemBarsVisibilityController {
    return remember {
        object : SystemBarsVisibilityController {
            override fun show() = notSupport("Hide systemBars")
            override fun hide() = notSupport("Hide systemBars")
        }
    }
}