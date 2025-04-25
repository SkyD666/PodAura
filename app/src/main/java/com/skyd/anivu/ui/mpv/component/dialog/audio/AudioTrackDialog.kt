package com.skyd.anivu.ui.mpv.component.dialog.audio

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.skyd.anivu.ext.safeLaunch
import com.skyd.anivu.ui.component.PodAuraIconButton
import com.skyd.anivu.ui.mpv.component.dialog.BasicPlayerDialog
import com.skyd.anivu.ui.mpv.component.dialog.DelayMillisDialog
import com.skyd.anivu.ui.mpv.component.dialog.TrackDialogListItem
import com.skyd.anivu.ui.mpv.component.state.PlayState
import com.skyd.anivu.ui.mpv.component.state.dialog.OnDialogVisibilityChanged
import com.skyd.anivu.ui.mpv.component.state.dialog.track.AudioTrackDialogCallback
import com.skyd.anivu.ui.mpv.component.state.dialog.track.AudioTrackDialogState
import com.skyd.anivu.ui.mpv.resolveUri
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.item_selected
import podaura.shared.generated.resources.player_add_external_audio
import podaura.shared.generated.resources.player_audio_delay
import podaura.shared.generated.resources.player_audio_track
import podaura.shared.generated.resources.settings


@Composable
internal fun AudioTrackDialog(
    onDismissRequest: () -> Unit,
    playState: () -> PlayState,
    audioTrackDialogState: () -> AudioTrackDialogState,
    audioTrackDialogCallback: AudioTrackDialogCallback,
    onDialogVisibilityChanged: OnDialogVisibilityChanged,
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
                        text = stringResource(Res.string.player_audio_track),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    PodAuraIconButton(
                        onClick = { onDialogVisibilityChanged.onAudioSettingDialog(true) },
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(Res.string.settings),
                    )
                    PodAuraIconButton(
                        onClick = { pickAudioFileLauncher.safeLaunch("*/*") },
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(Res.string.player_add_external_audio),
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
                            Icons.Outlined.Check else null,
                        iconContentDescription = stringResource(Res.string.item_selected),
                        text = track.name,
                        onClick = { audioTrackDialogCallback.onAudioTrackChanged(track) }
                    )
                }
            }
        }
    }

    if (state.showSetting) {
        val currentPlayState = playState()
        DelayMillisDialog(
            title = stringResource(Res.string.player_audio_delay),
            delay = currentPlayState.audioDelay,
            onConform = { audioTrackDialogCallback.onAudioDelayChanged(it) },
            onDismiss = { onDialogVisibilityChanged.onAudioSettingDialog(false) },
        )
    }
}