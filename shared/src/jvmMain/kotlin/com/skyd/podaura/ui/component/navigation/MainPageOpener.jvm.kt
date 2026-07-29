package com.skyd.podaura.ui.component.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.skyd.podaura.ui.window.LocalDesktopAppState

@Composable
actual fun rememberMainPageOpener(): MainPageOpener {
    val appState = LocalDesktopAppState.current
    return remember(appState) {
        object : MainPageOpener {
            override fun open(deeplink: String) = appState.openMainPage(deeplink)
        }
    }
}
