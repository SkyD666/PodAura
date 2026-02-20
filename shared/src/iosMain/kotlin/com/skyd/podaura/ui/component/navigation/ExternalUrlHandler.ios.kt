package com.skyd.podaura.ui.component.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey

@Composable
actual fun ExternalUrlListener(navBackStack: MutableList<NavKey>) {
    DefaultUrlListener(navBackStack = navBackStack)
}

@Composable
actual fun initialNavKey(): NavKey? {
    // todo
    return null
}