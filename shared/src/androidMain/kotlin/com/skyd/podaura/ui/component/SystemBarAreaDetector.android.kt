package com.skyd.podaura.ui.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.window.layout.WindowMetricsCalculator
import com.skyd.podaura.ext.toRect

@Composable
actual fun rememberSystemBarAreaDetector(): SystemBarAreaDetector {
    val context = LocalContext.current
    val safeGestures by rememberUpdatedState(newValue = WindowInsets.safeGestures.toRect())
    val density = LocalDensity.current
    return remember(context) {
        object : SystemBarAreaDetector {
            override fun inSystemBarArea(x: Float, y: Float): Boolean {
                val bounds = WindowMetricsCalculator
                    .getOrCreate()
                    .computeCurrentWindowMetrics(context)
                    .bounds
                val width = bounds.width()
                val height = bounds.height()
                val inGesturesArea = x <= safeGestures.left ||
                        x >= width - safeGestures.right ||
                        y <= safeGestures.top ||
                        y >= height - safeGestures.bottom
                val density = density.density
                return inGesturesArea || y / density <= 60 || (width - x) / density <= 60
            }
        }
    }
}