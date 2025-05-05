package com.skyd.podaura.ui.player.component.state.dialog.track

import androidx.compose.runtime.Immutable
import com.skyd.podaura.ui.player.Track

data class AudioTrackDialogState(
    val show: Boolean,
    val showSetting: Boolean,
) {
    companion object {
        val initial = AudioTrackDialogState(
            show = false,
            showSetting = false,
        )
    }
}

@Immutable
data class AudioTrackDialogCallback(
    val onAudioTrackChanged: (Track) -> Unit,
    val onAddAudioTrack: (String) -> Unit,
    val onAudioDelayChanged: (Long) -> Unit,
)