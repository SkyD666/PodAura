package com.skyd.podaura.ui.component.navigation

import androidx.compose.runtime.Composable

/**
 * Opens an internal PodAura page in the application's main window.
 *
 * The caller owns route construction so this API can support any page that exposes a deeplink.
 */
interface MainPageOpener {
    fun open(deeplink: String)
}

@Composable
expect fun rememberMainPageOpener(): MainPageOpener
