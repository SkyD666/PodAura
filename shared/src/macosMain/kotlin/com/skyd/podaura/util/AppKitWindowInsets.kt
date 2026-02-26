package com.skyd.podaura.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalPlatformWindowInsets
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.PlatformWindowInsets
import kotlinx.cinterop.useContents
import platform.AppKit.NSWindow

private class AppKitWindowInsets(private val window: () -> NSWindow) : PlatformWindowInsets {
    override val captionBar: PlatformInsets
        get() {
            val window = window()
            val windowFrameHeight = window.frame.useContents { size.height }
            val contentFrameHeight = window.contentLayoutRect.useContents { size.height }
            return PlatformInsets(
                top = (windowFrameHeight - contentFrameHeight).toInt()
            )
        }
    override val systemBars: PlatformInsets
        get() = captionBar
}

@Composable
fun ProvidePlatformWindowInsets(
    window: () -> NSWindow,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalPlatformWindowInsets provides AppKitWindowInsets(window),
        content = content
    )
}
