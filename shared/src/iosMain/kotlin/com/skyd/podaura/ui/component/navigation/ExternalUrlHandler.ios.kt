package com.skyd.podaura.ui.component.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.NavKey
import com.skyd.podaura.ui.notification.iosArticleNotificationRequests
import com.skyd.podaura.ui.player.LocalIosPlayerSession
import com.skyd.podaura.ui.player.PlayerOpenRequest
import com.skyd.podaura.ui.screen.settings.data.importexport.importopml.ImportOpmlRoute
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension
import kotlinx.coroutines.channels.Channel
import platform.Foundation.NSURL

// Retain the original NSURL (including its security scope) until navigation is ready.
internal val iosDocumentRequests = Channel<PlatformFile>(Channel.UNLIMITED)

/** Called by SwiftUI for both cold-launch and running-app document events. */
@Suppress("unused")
fun openIosDocument(url: NSURL) {
    if (url.isFileURL()) iosDocumentRequests.trySend(PlatformFile(url))
}

internal fun PlatformFile.opmlImportRoute(): ImportOpmlRoute? =
    when (extension.lowercase()) {
        "opml", "xml" -> ImportOpmlRoute(opmlFile = this)
        else -> null
    }

@Composable
actual fun ExternalUrlListener(navBackStack: MutableList<NavKey>) {
    val player = LocalIosPlayerSession.current
    val navigation = LocalIosAppNavigation.current
    DisposableEffect(navigation) {
        ExternalUrlHandler.listener = { data ->
            data.toNavKey()?.let { route ->
                navigation.openPage(route)
            }
        }
        onDispose { ExternalUrlHandler.listener = null }
    }
    LaunchedEffect(navigation) {
        for (route in iosArticleNotificationRequests) {
            navigation.openPage(route)
        }
    }
    LaunchedEffect(navigation, player) {
        for (file in iosDocumentRequests) {
            val importRoute = file.opmlImportRoute()
            if (importRoute != null) {
                navigation.openPage(importRoute)
            } else {
                player.open(PlayerOpenRequest.Files(listOf(file)))
            }
        }
    }
}

@Composable
actual fun initialNavKey(): NavKey? {
    // todo
    return null
}
