package com.skyd.anivu.ui.mpv.component.dialog

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skyd.anivu.R
import com.skyd.anivu.ext.safeLaunch
import com.skyd.anivu.ui.component.PodAuraIconButton
import com.skyd.anivu.ui.mpv.component.state.PlayState
import com.skyd.anivu.ui.mpv.component.state.dialog.track.AudioTrackDialogCallback
import com.skyd.anivu.ui.mpv.component.state.dialog.track.AudioTrackDialogState
import com.skyd.anivu.ui.mpv.resolveUri


@Composable
internal fun AudioTrackDialog(
    onDismissRequest: () -> Unit,
    playState: () -> PlayState,
    audioTrackDialogState: () -> AudioTrackDialogState,
    audioTrackDialogCallback: AudioTrackDialogCallback,
) {
    val state = audioTrackDialogState()
    val context = LocalContext.current
    val pickAudioFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { subtitleUri ->
        subtitleUri?.resolveUri(context)?.let { filePath ->
            audioTrackDialogCallback.onAddAudioTrack(filePath)
        }
    }

    if (state.show) {
        BasicPlayerDialog(onDismissRequest = onDismissRequest) {
            Column(
                modifier = Modifier
                    .padding(PaddingValues(16.dp))
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp, bottom = 6.dp),
                        text = stringResource(id = R.string.player_audio_track),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    PodAuraIconButton(
                        onClick = { pickAudioFileLauncher.safeLaunch("*/*") },
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(id = R.string.player_add_external_audio),
                    )
                }
                val currentPlayState = playState()
                repeat(currentPlayState.audioTracks.size) { index ->
                    val track = currentPlayState.audioTracks[index]
                    val currentTrack = currentPlayState.audioTracks.find {
                        it.trackId == currentPlayState.audioTrackId
                    }
                    TrackDialogListItem(
                        imageVector = if (currentTrack?.trackId == track.trackId)
                            Icons.Rounded.Check else null,
                        iconContentDescription = stringResource(id = R.string.item_selected),
                        text = track.name,
                        onClick = { audioTrackDialogCallback.onAudioTrackChanged(track) }
                    )
                }
            }
        }
    }
}