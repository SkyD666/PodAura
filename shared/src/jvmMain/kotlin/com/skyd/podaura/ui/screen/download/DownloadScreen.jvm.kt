package com.skyd.podaura.ui.screen.download

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.navDeepLink
import kotlinx.serialization.Serializable

@Serializable
actual object DownloadDeepLinkRoute {
    @Composable
    actual fun DownloadDeepLinkLauncher(entry: NavBackStackEntry) {
    }

    actual val deepLinks: List<NavDeepLink> =
        listOf("magnet:.*", "http://.*", "https://.*", "file://.*").map {
            navDeepLink {
                uriPattern = it
            }
        }
}