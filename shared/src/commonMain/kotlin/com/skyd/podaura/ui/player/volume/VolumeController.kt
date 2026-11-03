package com.skyd.podaura.ui.player.volume

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.staticCompositionLocalOf

/** Shared by mobile system-volume gestures and desktop application-volume controls. */
interface VolumeController {
    val available: Boolean get() = true
    val range: ClosedFloatingPointRange<Float>
    var value: Float
    val muted: Boolean get() = value <= range.start
    fun toggleMute() {
        value = if (muted) range.endInclusive else range.start
    }
}

fun VolumeController.adjustTo(value: Float) {
    if (value.isFinite()) this.value = value.coerceIn(range)
}

@Composable
expect fun rememberVolumeController(): VolumeController

/** Window shortcuts must yield while a volume control owns keyboard focus. */
internal val LocalVolumeControlFocus = staticCompositionLocalOf<MutableState<Boolean>?> { null }
