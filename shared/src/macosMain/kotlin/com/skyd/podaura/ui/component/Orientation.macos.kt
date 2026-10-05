package com.skyd.podaura.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalWindowInfo

@Composable
actual fun isLandscape(): Boolean =
    LocalWindowInfo.current.containerSize.let { it.width > it.height }

@Composable
actual fun rememberOrientationController(): OrientationController = remember {
    object : OrientationController {
        override fun landscape() = Unit
        override fun unspecified() = Unit
    }
}
