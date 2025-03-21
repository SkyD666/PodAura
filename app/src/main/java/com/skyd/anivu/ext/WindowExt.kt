package com.skyd.anivu.ext

import android.graphics.Rect
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection

val WindowSizeClass.isCompact: Boolean
    get() = widthSizeClass == WindowWidthSizeClass.Compact

val WindowSizeClass.isMedium: Boolean
    get() = widthSizeClass == WindowWidthSizeClass.Medium

val WindowSizeClass.isExpanded: Boolean
    get() = widthSizeClass == WindowWidthSizeClass.Expanded

@Composable
fun WindowInsets.toRect(): Rect {
    val density = LocalDensity.current
    return Rect(
        getLeft(density, LocalLayoutDirection.current),
        getTop(density),
        getRight(density, LocalLayoutDirection.current),
        getBottom(density)
    )
}