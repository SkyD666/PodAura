package com.skyd.podaura.ui.component.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri

private const val MAIN_ACTIVITY_CLASS_NAME = "com.skyd.podaura.ui.activity.MainActivity"

@Composable
actual fun rememberMainPageOpener(): MainPageOpener {
    val context = LocalContext.current
    return remember(context) {
        object : MainPageOpener {
            override fun open(deeplink: String) {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        deeplink.toUri(),
                        context,
                        Class.forName(MAIN_ACTIVITY_CLASS_NAME),
                    )
                )
            }
        }
    }
}
