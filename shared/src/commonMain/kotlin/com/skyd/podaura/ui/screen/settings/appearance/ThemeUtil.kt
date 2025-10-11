package com.skyd.podaura.ui.screen.settings.appearance

import androidx.compose.runtime.Composable

interface PlatformThemeOperator {
    val isDynamicColorAvailable: Boolean
    fun onThemeChanged()
}

object DefaultPlatformThemeOperator : PlatformThemeOperator {
    override val isDynamicColorAvailable: Boolean get() = false
    override fun onThemeChanged() = Unit
}

@Composable
expect fun rememberPlatformThemeOperator(): PlatformThemeOperator