package com.skyd.podaura.ui.screen.settings.data.importexport.importopml

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController

@Composable
actual fun ImportOpmlDeepLinkLauncher(entry: NavBackStackEntry) {
    val intent = entry.arguments?.getParcelable<Intent>(NavController.KEY_DEEP_LINK_INTENT)
    ImportOpmlScreen(opmlUrl = intent?.clipData?.takeIf { it.itemCount > 0 }
        ?.getItemAt(0)?.uri?.toString())
}