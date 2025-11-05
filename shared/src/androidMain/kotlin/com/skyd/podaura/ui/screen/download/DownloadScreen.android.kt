package com.skyd.podaura.ui.screen.download

import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDeepLink
import androidx.navigation.navDeepLink
import com.skyd.podaura.ext.type
import kotlinx.serialization.Serializable

@Serializable
actual object DownloadDeepLinkRoute {
    @Composable
    actual fun DownloadDeepLinkLauncher(entry: NavBackStackEntry) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            entry.arguments?.getParcelable(NavController.KEY_DEEP_LINK_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            entry.arguments?.getParcelable(NavController.KEY_DEEP_LINK_INTENT)
        }
        DownloadScreen(downloadLink = intent?.data?.toString(), mimetype = intent?.data?.type)
    }

    actual val deepLinks: List<NavDeepLink> =
        listOf("magnet:.*", "http://.*", "https://.*", "file://.*").map {
            navDeepLink {
                action = Intent.ACTION_VIEW
                uriPattern = it
            }
        }
}