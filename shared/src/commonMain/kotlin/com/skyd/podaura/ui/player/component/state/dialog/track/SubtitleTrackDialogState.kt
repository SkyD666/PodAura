package com.skyd.podaura.ui.player.component.state.dialog.track

import androidx.compose.runtime.Immutable
import com.skyd.podaura.ui.player.Track

data class SubtitleTrackDialogState(
    val show: Boolean,
    val showSetting: Boolean,
) {
    companion object {
        val initial = SubtitleTrackDialogState(
            show = false,
            showSetting = false,
        )
    }
}

@Immutable
data class SubtitleTrackDialogCallback(
    val onSubtitleTrackChanged: (Track) -> Unit,
    val onAddSubtitle: (String) -> Unit,
    val onSubtitleDelayChanged: (Long) -> Unit,
)