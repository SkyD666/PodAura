package com.skyd.podaura.ui.screen.settings.appearance

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.skyd.podaura.util.DynamicColorUtil

@Composable
actual fun rememberPlatformThemeOperator(): PlatformThemeOperator {
    val activity = LocalActivity.current

    return remember(activity) {
        object : PlatformThemeOperator {
            override val isDynamicColorAvailable: Boolean
                get() = DynamicColorUtil.isDynamicColorAvailable()

            override fun onThemeChanged() {
                activity?.recreate()
            }
        }
    }
}
