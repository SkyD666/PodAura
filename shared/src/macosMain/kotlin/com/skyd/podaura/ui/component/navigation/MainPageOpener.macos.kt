package com.skyd.podaura.ui.component.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.skyd.podaura.ui.local.LocalMacosMainWindow
import platform.AppKit.NSApplication

@Composable
actual fun rememberMainPageOpener(): MainPageOpener {
    val window = LocalMacosMainWindow.current
    return remember(window) {
        object : MainPageOpener {
            override fun open(deeplink: String) {
                ExternalUrlHandler.onNewUrl(ExternalUrlHandler.UrlData(url = deeplink))
                window?.let {
                    window.deminiaturize(null)
                    window.makeKeyAndOrderFront(null)
                    NSApplication.sharedApplication.activateIgnoringOtherApps(true)
                }
            }
        }
    }
}
