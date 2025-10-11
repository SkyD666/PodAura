package com.skyd.podaura.ui.player.component.dialog.audio

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
import androidx.compose.ui.unit.dp
import com.skyd.compone.component.ComponeIconButton
import com.skyd.podaura.ui.player.component.dialog.BasicPlayerDialog
import com.skyd.podaura.ui.player.component.dialog.DelayMillisDialog
import com.skyd.podaura.ui.player.component.dialog.TrackDialogListItem
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.dialog.OnDialogVisibilityChanged
import com.skyd.podaura.ui.player.component.state.dialog.track.AudioTrackDialogCallback
import com.skyd.podaura.ui.player.component.state.dialog.track.AudioTrackDialogState
import com.skyd.podaura.ui.player.resolveToPlayer
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.item_selected
import podaura.shared.generated.resources.player_add_external_audio
import podaura.shared.generated.resources.player_audio_delay
import podaura.shared.generated.resources.player_audio_track
import podaura.shared.generated.resources.settings


@Composable
/*internal*/ fun AudioTrackDialog(
    onDismissRequest: () -> Unit,
    playState: () -> PlayState,
    audioTrackDialogState: () -> AudioTrackDialogState,
    audioTrackDialogCallback: AudioTrackDialogCallback,
    onDialogVisibilityChanged: OnDialogVisibilityChanged,
) {
    val state = audioTrackDialogState()
    val pickAudioFileLauncher = rememberFilePickerLauncher { subtitleFile ->
        subtitleFile?.resolveToPlayer()?.let { filePath ->
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
                    ComponeIconButton(
                        onClick = { onDialogVisibilityChanged.onAudioSettingDialog(true) },
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(Res.string.settings),
                    )
                    ComponeIconButton(
                        onClick = { pickAudioFileLauncher.launch() },
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