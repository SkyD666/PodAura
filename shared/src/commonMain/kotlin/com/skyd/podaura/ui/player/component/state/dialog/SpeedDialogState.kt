package com.skyd.podaura.ui.player.component.state.dialog

import androidx.compose.runtime.Immutable

data class SpeedDialogState(
    val show: Boolean,
) {
    companion object {
        val initial = SpeedDialogState(
            show = false,
        )
    }
}

@Immutable
data class SpeedDialogCallback(
    val onSpeedChanged: (Float) -> Unit,
)