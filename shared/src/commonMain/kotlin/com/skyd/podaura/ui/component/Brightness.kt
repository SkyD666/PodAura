package com.skyd.podaura.ui.component

import androidx.compose.runtime.Composable

interface BrightnessController {
    var percent: Float
}

@Composable
expect fun rememberBrightnessController(): BrightnessController