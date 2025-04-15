package com.skyd.anivu.ui.mpv.component.state.dialog.track

import androidx.compose.runtime.Immutable
import com.skyd.anivu.ui.mpv.MPVPlayer

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
    val onAudioTrackChanged: (MPVPlayer.Track) -> Unit,
    val onAddAudioTrack: (String) -> Unit,
    val onAudioDelayChanged: (Long) -> Unit,
)