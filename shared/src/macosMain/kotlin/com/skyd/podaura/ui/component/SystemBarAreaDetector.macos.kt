package com.skyd.podaura.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberSystemBarAreaDetector(): SystemBarAreaDetector {
    return remember {
        object : SystemBarAreaDetector {
            override fun inSystemBarArea(x: Float, y: Float): Boolean = false
        }
    }
}
