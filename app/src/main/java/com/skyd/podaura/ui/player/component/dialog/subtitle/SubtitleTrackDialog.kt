package com.skyd.podaura.ui.player.component.dialog.subtitle

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
import com.skyd.podaura.ext.safeLaunch
import com.skyd.podaura.ui.component.PodAuraIconButton
import com.skyd.podaura.ui.player.component.dialog.BasicPlayerDialog
import com.skyd.podaura.ui.player.component.dialog.DelayMillisDialog
import com.skyd.podaura.ui.player.component.dialog.TrackDialogListItem
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.dialog.OnDialogVisibilityChanged
import com.skyd.podaura.ui.player.component.state.dialog.track.SubtitleTrackDialogCallback
import com.skyd.podaura.ui.player.component.state.dialog.track.SubtitleTrackDialogState
import com.skyd.podaura.ui.player.resolveUri
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.item_selected
import podaura.shared.generated.resources.player_add_external_subtitle
import podaura.shared.generated.resources.player_subtitle_delay
import podaura.shared.generated.resources.player_subtitle_track
import podaura.shared.generated.resources.settings


@Composable
internal fun SubtitleTrackDialog(
    onDismissRequest: () -> Unit,
    playState: () -> PlayState,
    subtitleTrackDialogState: () -> SubtitleTrackDialogState,
    subtitleTrackDialogCallback: SubtitleTrackDialogCallback,
    onDialogVisibilityChanged: OnDialogVisibilityChanged,
) {
    val state = subtitleTrackDialogState()
    val context = LocalContext.current
    val pickSubtitleFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { subtitleUri ->
        subtitleUri?.resolveUri(context)?.let { filePath ->
            subtitleTrackDialogCallback.onAddSubtitle(filePath)
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
                        text = stringResource(Res.string.player_subtitle_track),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    PodAuraIconButton(
                        onClick = { onDialogVisibilityChanged.onSubtitleSettingDialog(true) },
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(Res.string.settings),
                    )
                    PodAuraIconButton(
                        onClick = { pickSubtitleFileLauncher.safeLaunch("*/*") },
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(Res.string.player_add_external_subtitle),
                    )
                }
                val currentPlayState = playState()
                repeat(currentPlayState.subtitleTracks.size) { index ->
                    val track = currentPlayState.subtitleTracks[index]
                    val currentTrack = currentPlayState.subtitleTracks.find {
                        it.trackId == currentPlayState.subtitleTrackId
                    }
                    TrackDialogListItem(
                        imageVector = if (currentTrack?.trackId == track.trackId)
                            Icons.Outlined.Check else null,
                        iconContentDescription = stringResource(Res.string.item_selected),
                        text = track.name,
                        onClick = { subtitleTrackDialogCallback.onSubtitleTrackChanged(track) }
                    )
                }
            }
        }
    }

    if (state.showSetting) {
        val currentPlayState = playState()
        DelayMillisDialog(
            title = stringResource(Res.string.player_subtitle_delay),
            delay = currentPlayState.subTitleDelay,
            onConform = { subtitleTrackDialogCallback.onSubtitleDelayChanged(it) },
            onDismiss = { onDialogVisibilityChanged.onSubtitleSettingDialog(false) },
        )
    }
}