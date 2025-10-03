package com.skyd.podaura.ui.player.land

import androidx.compose.runtime.Composable

interface SystemBarsVisibilityController {
    fun show()
    fun hide()
}

@Composable
expect fun rememberSystemBarsVisibilityController(): SystemBarsVisibilityController