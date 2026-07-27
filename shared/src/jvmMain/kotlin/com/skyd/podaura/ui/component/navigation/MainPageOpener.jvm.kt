package com.skyd.podaura.ui.component.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.WindowState
import com.skyd.podaura.ui.component.navigation.ExternalUrlHandler.UrlData

private object MainWindowRegistry {
    private var window: ComposeWindow? = null
    private var windowState: WindowState? = null

    fun register(window: ComposeWindow, windowState: WindowState) {
        this.window = window
        this.windowState = windowState
    }

    fun unregister(window: ComposeWindow) {
        if (this.window === window) {
            this.window = null
            windowState = null
        }
    }

    fun bringToFront() {
        windowState?.isMinimized = false
        window?.apply {
            toFront()
            requestFocus()
        }
    }
}

internal fun registerMainWindow(window: ComposeWindow, windowState: WindowState) {
    MainWindowRegistry.register(window, windowState)
}

internal fun unregisterMainWindow(window: ComposeWindow) {
    MainWindowRegistry.unregister(window)
}

@Composable
actual fun rememberMainPageOpener(): MainPageOpener = remember {
    object : MainPageOpener {
        override fun open(deeplink: String) {
            ExternalUrlHandler.onNewUrl(UrlData(url = deeplink))
            MainWindowRegistry.bringToFront()
        }
    }
}
