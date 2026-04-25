package com.skyd.podaura.ui.component.frame.macos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalPlatformWindowInsets
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.PlatformWindowInsets
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import com.skyd.fundation.jna.mac.CGRect
import com.skyd.fundation.jna.mac.ObjCRuntime
import com.sun.jna.Pointer

@Composable
fun FrameWindowScope.MacOSWindowFrame(
    content: @Composable () -> Unit
) {
    SideEffect {
        window.rootPane.putClientProperty("apple.awt.application.appearance", "system")
        window.rootPane.putClientProperty("apple.awt.fullscreenable", true)
        window.rootPane.putClientProperty("apple.awt.windowTitleVisible", false)
        window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
        window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
    }

    val density = LocalDensity.current
    val windowInsets = remember {
        object : PlatformWindowInsets {
            override val systemBars: PlatformInsets
                get() {
                    val window = Pointer(window.windowHandle)
                    val frame: CGRect = ObjCRuntime.msgSend(window, "frame")
                    val layout: CGRect = ObjCRuntime.msgSend(window, "contentLayoutRect")
                    val height = frame.height - layout.height
                    return density.PlatformInsets(top = height.dp)
                }
        }
    }

    CompositionLocalProvider(
        LocalPlatformWindowInsets provides windowInsets,
        content = content
    )
}
