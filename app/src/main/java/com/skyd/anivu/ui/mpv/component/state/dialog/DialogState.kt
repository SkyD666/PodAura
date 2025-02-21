package com.skyd.anivu.ui.mpv.component.state.dialog

import androidx.compose.runtime.Immutable
import com.skyd.anivu.ui.mpv.component.state.dialog.track.AudioTrackDialogCallback
import com.skyd.anivu.ui.mpv.component.state.dialog.track.AudioTrackDialogState
import com.skyd.anivu.ui.mpv.component.state.dialog.track.SubtitleTrackDialogCallback
import com.skyd.anivu.ui.mpv.component.state.dialog.track.SubtitleTrackDialogState

@Immutable
data class DialogState(
    val speedDialogState: () -> SpeedDialogState,
    val audioTrackDialogState: () -> AudioTrackDialogState,
    val subtitleTrackDialogState: () -> SubtitleTrackDialogState,
) {
    companion object {
        val initial = DialogState(
            speedDialogState = { SpeedDialogState.initial },
            audioTrackDialogState = { AudioTrackDialogState.initial },
            subtitleTrackDialogState = { SubtitleTrackDialogState.initial },
        )
    }
}

@Immutable
data class DialogCallback(
    val speedDialogCallback: SpeedDialogCallback,
    val audioTrackDialogCallback: AudioTrackDialogCallback,
    val subtitleTrackDialogCallback: SubtitleTrackDialogCallback,
)

@Immutable
data class OnDialogVisibilityChanged(
    val onSpeedDialog: (Boolean) -> Unit,
    val onSubtitleTrackDialog: (Boolean) -> Unit,
    val onAudioTrackDialog: (Boolean) -> Unit,
)