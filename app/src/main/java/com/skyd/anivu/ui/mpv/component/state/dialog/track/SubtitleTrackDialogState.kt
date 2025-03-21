package com.skyd.anivu.ui.mpv.component.state.dialog.track

import androidx.compose.runtime.Immutable
import com.skyd.anivu.ui.mpv.MPVPlayer

data class SubtitleTrackDialogState(
    val show: Boolean,
) {
    companion object {
        val initial = SubtitleTrackDialogState(
            show = false,
        )
    }
}

@Immutable
data class SubtitleTrackDialogCallback(
    val onSubtitleTrackChanged: (MPVPlayer.Track) -> Unit,
    val onAddSubtitle: (String) -> Unit,
)