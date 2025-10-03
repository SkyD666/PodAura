package com.skyd.podaura.ui.component

import androidx.compose.runtime.Composable

interface AudioController {
    val range: ClosedFloatingPointRange<Float>
    var value: Float
}

@Composable
expect fun rememberAudioController(): AudioController