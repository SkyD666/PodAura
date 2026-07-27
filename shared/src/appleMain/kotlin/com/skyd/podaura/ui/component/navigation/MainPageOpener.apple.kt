package com.skyd.podaura.ui.component.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberMainPageOpener(): MainPageOpener = remember {
    object : MainPageOpener {
        override fun open(deeplink: String) = Unit
    }
}
