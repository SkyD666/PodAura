package com.skyd.podaura.ui.component

import androidx.compose.runtime.Composable

interface SystemBarAreaDetector {
    fun inSystemBarArea(x: Float, y: Float): Boolean
}

@Composable
expect fun rememberSystemBarAreaDetector(): SystemBarAreaDetector