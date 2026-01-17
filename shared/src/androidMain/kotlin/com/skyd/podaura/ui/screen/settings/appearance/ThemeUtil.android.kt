package com.skyd.podaura.ui.screen.settings.appearance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.skyd.podaura.ext.activity
import com.skyd.podaura.util.DynamicColorUtil

@Composable
actual fun rememberPlatformThemeOperator(): PlatformThemeOperator {
    val context = LocalContext.current
    return remember(context) {
        object : PlatformThemeOperator {
            override val isDynamicColorAvailable: Boolean
                get() = DynamicColorUtil.isDynamicColorAvailable()

            override fun onThemeChanged() {
                context.activity.recreate()
            }
        }
    }
}