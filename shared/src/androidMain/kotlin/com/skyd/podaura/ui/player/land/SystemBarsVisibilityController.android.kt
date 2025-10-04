package com.skyd.podaura.ui.player.land

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.view.WindowInsetsControllerCompat
import com.skyd.podaura.ui.component.rememberSystemUiController

@Composable
actual fun rememberSystemBarsVisibilityController(): SystemBarsVisibilityController {
    val systemUiController = rememberSystemUiController()
    return remember {
        object : SystemBarsVisibilityController {
            override fun show() {
                with(systemUiController) {
                    isSystemBarsVisible = true
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                }
            }

            override fun hide() {
                with(systemUiController) {
                    isSystemBarsVisible = false
                    systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        }
    }
}