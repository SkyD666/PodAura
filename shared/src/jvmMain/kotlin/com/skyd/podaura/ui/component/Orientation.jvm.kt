package com.skyd.podaura.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// Desktop platforms do not offer landscape mode.
@Composable
actual fun isLandscape(): Boolean = false

@Composable
actual fun rememberOrientationController(): OrientationController = remember {
    object : OrientationController {
        override fun landscape() = Unit
        override fun unspecified() = Unit
    }
}