package com.skyd.podaura.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState

/**
 * The standard top-level window for the JVM application.
 *
 * Keeps behavior shared by regular PodAura windows in one place.
 */
@Composable
internal fun BaseWindow(
    onCloseRequest: () -> Unit,
    state: WindowState = rememberWindowState(),
    visible: Boolean = true,
    title: String = "Untitled",
    undecorated: Boolean = false,
    transparent: Boolean = false,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    alwaysOnTop: Boolean = false,
    onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    onKeyEvent: (KeyEvent) -> Boolean = { false },
    content: @Composable FrameWindowScope.() -> Unit,
) {
    // SwingWindow installs these listeners only when its native window is created.
    val currentOnPreviewKeyEvent by rememberUpdatedState(onPreviewKeyEvent)
    val currentOnKeyEvent by rememberUpdatedState(onKeyEvent)
    Window(
        onCloseRequest = onCloseRequest,
        state = state,
        visible = visible,
        title = title,
        undecorated = undecorated,
        transparent = transparent,
        resizable = resizable,
        enabled = enabled,
        focusable = focusable,
        alwaysOnTop = alwaysOnTop,
        onPreviewKeyEvent = { currentOnPreviewKeyEvent(it) },
        onKeyEvent = { currentOnKeyEvent(it) },
    ) {
        ApplyPodAuraWindowIcon()
        content()
    }
}
