package com.skyd.podaura.ui.component.navigation

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.util.Consumer
import androidx.navigation3.runtime.NavKey
import com.skyd.podaura.ext.activity
import com.skyd.podaura.ui.component.navigation.ExternalUrlHandler.UrlData


@Composable
actual fun ExternalUrlListener(navBackStack: MutableList<NavKey>) {
    val activity = LocalContext.current.activity as ComponentActivity
    DefaultUrlListener(navBackStack = navBackStack)
    DisposableEffect(navBackStack) {
        val listener = Consumer<Intent> { intent ->
            ExternalUrlHandler.onNewUrl(data = intent.toUrlData())
        }
        activity.addOnNewIntentListener(listener)
        onDispose {
            activity.removeOnNewIntentListener(listener)
        }
    }
}

private fun Intent.toUrlData(): UrlData = UrlData(
    url = data?.toString(),
    mimeType = type,
    action = action,
)

@Composable
actual fun initialNavKey(): NavKey? {
    var onCreateIntentResolved = rememberSaveable { false }
    if (!onCreateIntentResolved) {
        onCreateIntentResolved = true
        val activity = LocalContext.current.activity as ComponentActivity
        return activity.intent?.toUrlData()?.toNavKey()
    }
    return null
}