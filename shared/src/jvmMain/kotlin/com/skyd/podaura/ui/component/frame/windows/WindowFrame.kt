package com.skyd.podaura.ui.component.frame.windows

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalPlatformWindowInsets
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.PlatformWindowInsets
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState

@Composable
fun FrameWindowScope.WindowsWindowFrame(
    onCloseRequest: () -> Unit,
    state: WindowState,
    content: @Composable () -> Unit
) {
    val layoutHitTestOwner = rememberLayoutHitTestOwner()
    val procedure = remember { ExtendedTitleBarWindowProc(window) }

    DisposableEffect(Unit) {
        onDispose {
            procedure.close()
        }
    }

    // 0 is minimize, 1 is maximize, 2 is close
    val captionButtonsRect = remember { Array(3) { Rect.Zero } }
    var captionButtonsSize by remember { mutableStateOf(IntSize(0, 0)) }

    LaunchedEffect(this, procedure) {
        procedure.updateChildHitTestProvider { x, y ->
            when {
                captionButtonsRect[0].contains(x, y) -> WindowsWindowHitResult.CAPTION_MIN
                captionButtonsRect[1].contains(x, y) -> WindowsWindowHitResult.CAPTION_MAX
                captionButtonsRect[2].contains(x, y) -> WindowsWindowHitResult.CAPTION_CLOSE
                y <= captionButtonsSize.height && !layoutHitTestOwner.hitTest(x, y)
                    -> WindowsWindowHitResult.CAPTION

                else -> WindowsWindowHitResult.CLIENT
            }
        }
    }

    val isActive by procedure.windowIsActive.collectAsState()
    val frameIsColorful by procedure.frameIsColorful.collectAsState()
    val accentColor by procedure.accentColor.collectAsState()

    // Keep 1px for showing float window top area border on Windows 10
    val topBorderFixedInsets by remember {
        derivedStateOf {
            val isFloatingWindow = state.placement == WindowPlacement.Floating
            if (isFloatingWindow) WindowInsets(top = 1) else WindowInsets()
        }
    }

    val density = LocalDensity.current
    val windowInsets = remember {
        object : PlatformWindowInsets {
            override val systemBars: PlatformInsets
                get() = density.PlatformInsets(top = 32.dp)
        }
    }

    Box(
        modifier = Modifier.windowInsetsPadding(topBorderFixedInsets).clipToBounds()
    ) {
        CompositionLocalProvider(
            LocalPlatformWindowInsets provides windowInsets,
            content = content
        )
        Box(
            contentAlignment = Alignment.TopEnd,
            modifier = Modifier.fillMaxWidth()
        ) {
            CaptionButtonRow(
                windowHandle = procedure::windowHandle,
                isMaximize = state.placement == WindowPlacement.Maximized,
                isActive = isActive,
                accentColor = accentColor,
                frameColorEnabled = frameIsColorful,
                onCloseRequest = onCloseRequest,
                modifier = Modifier.onSizeChanged {
                    captionButtonsSize = it
                },
                onMinimizeButtonRectUpdate = {
                    captionButtonsRect[0] = it
                },
                onMaximizeButtonRectUpdate = {
                    captionButtonsRect[1] = it
                },
                onCloseButtonRectUpdate = {
                    captionButtonsRect[2] = it
                }
            )
        }
    }
}

fun Rect.contains(x: Float, y: Float): Boolean {
    return x in left..<right && y in top..<bottom
}
