package com.skyd.podaura.ui.component.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberMainPageOpener(): MainPageOpener {
    val navigation = LocalIosAppNavigation.current
    return remember(navigation) {
        object : MainPageOpener {
            override fun open(deeplink: String) {
                val route = ExternalUrlHandler.UrlData(url = deeplink).toNavKey() ?: return
                navigation.openPage(route)
            }
        }
    }
}
