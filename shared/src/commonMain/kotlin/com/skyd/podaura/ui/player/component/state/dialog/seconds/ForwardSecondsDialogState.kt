package com.skyd.podaura.ui.player.component.state.dialog.seconds

data class ForwardSecondsDialogState(
    val show: Boolean,
) {
    companion object {
        val initial = ForwardSecondsDialogState(
            show = false,
        )
    }
}