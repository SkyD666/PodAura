package com.skyd.podaura.ui.player.component.state.dialog.seconds

data class ReplaySecondsDialogState(
    val show: Boolean,
) {
    companion object {
        val initial = ReplaySecondsDialogState(
            show = false,
        )
    }
}