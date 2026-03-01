package com.skyd.podaura.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalPlatformWindowInsets
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.PlatformWindowInsets
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.CValue
import kotlinx.cinterop.useContents
import platform.AppKit.NSWindow
import platform.AppKit.NSWindowStyleMaskFullSizeContentView
import platform.AppKit.NSWindowTitleHidden
import platform.Foundation.NSEdgeInsets

private class AppKitWindowInsets(private val window: () -> NSWindow) : PlatformWindowInsets {
    override val systemBars: PlatformInsets
        get() {
            val window = window()
            val density = Density(window.backingScaleFactor.toFloat())
            val safeAreaInsets = window.contentView?.safeAreaInsets?.toPlatformInsets(density)
            return safeAreaInsets ?: PlatformInsets(0, 0, 0, 0)
        }
}

// Copied from https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/compose/ui/ui/src/iosMain/kotlin/androidx/compose/ui/unit/Conversions.ios.kt
private fun CValue<NSEdgeInsets>.toPlatformInsets(density: Density) = useContents {
    density.PlatformInsets(
        left = left.dp,
        top = top.dp,
        right = right.dp,
        bottom = bottom.dp
    )
}

@Composable
fun ProvidePlatformWindowInsets(
    window: () -> NSWindow,
    content: @Composable () -> Unit
) {
    val windowInsets = remember { AppKitWindowInsets(window) }

    SideEffect {
        val window = window()
        window.titlebarAppearsTransparent = true
        window.styleMask = window.styleMask or NSWindowStyleMaskFullSizeContentView
        window.titleVisibility = NSWindowTitleHidden
        window.contentView?.let {
            it.layer?.setBounds(it.bounds())
            it.setNeedsDisplay(true)
        }
    }

    CompositionLocalProvider(
        LocalPlatformWindowInsets provides windowInsets,
        content = content
    )
}
