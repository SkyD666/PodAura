package com.skyd.podaura.ui.screen.settings.data.importexport.importopml

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.core.os.BundleCompat
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController

@Composable
actual fun ImportOpmlDeepLinkLauncher(entry: NavBackStackEntry) {
    val intent = entry.arguments?.let {
        BundleCompat.getParcelable(
            it, NavController.KEY_DEEP_LINK_INTENT, Intent::class.java
        )
    }
    ImportOpmlScreen(opmlUrl = intent?.clipData?.takeIf { it.itemCount > 0 }
        ?.getItemAt(0)?.uri?.toString())
}