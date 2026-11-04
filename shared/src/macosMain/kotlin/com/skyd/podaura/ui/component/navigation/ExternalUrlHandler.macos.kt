package com.skyd.podaura.ui.component.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.NavKey
import com.skyd.podaura.ui.notification.appleArticleNotificationRequests

@Composable
actual fun ExternalUrlListener(navBackStack: MutableList<NavKey>) {
    DefaultUrlListener(navBackStack = navBackStack)
    val opener = rememberMainPageOpener()
    LaunchedEffect(opener) {
        for (route in appleArticleNotificationRequests) {
            opener.open(route.toDeeplink())
        }
    }
}

@Composable
actual fun initialNavKey(): NavKey? {
    // todo
    return null
}
