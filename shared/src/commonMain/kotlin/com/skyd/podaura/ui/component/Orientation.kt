package com.skyd.podaura.ui.component

import androidx.compose.runtime.Composable

@Composable
expect fun isLandscape(): Boolean

interface OrientationController {
    fun landscape()
    fun unspecified()
}

@Composable
expect fun rememberOrientationController(): OrientationController