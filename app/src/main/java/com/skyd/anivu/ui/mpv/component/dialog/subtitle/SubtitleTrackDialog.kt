package com.skyd.anivu.ui.mpv.component.dialog.subtitle

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skyd.anivu.R
import com.skyd.anivu.ext.safeLaunch
import com.skyd.anivu.ui.component.PodAuraIconButton
import com.skyd.anivu.ui.mpv.component.dialog.BasicPlayerDialog
import com.skyd.anivu.ui.mpv.component.dialog.DelayMillisDialog
import com.skyd.anivu.ui.mpv.component.dialog.TrackDialogListItem
import com.skyd.anivu.ui.mpv.component.state.PlayState
import com.skyd.anivu.ui.mpv.component.state.dialog.OnDialogVisibilityChanged
import com.skyd.anivu.ui.mpv.component.state.dialog.track.SubtitleTrackDialogCallback
import com.skyd.anivu.ui.mpv.component.state.dialog.track.SubtitleTrackDialogState
import com.skyd.anivu.ui.mpv.resolveUri


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
                        text = stringResource(id = R.string.player_subtitle_track),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    PodAuraIconButton(
                        onClick = { onDialogVisibilityChanged.onSubtitleSettingDialog(true) },
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(id = R.string.settings),
                    )
                    PodAuraIconButton(
                        onClick = { pickSubtitleFileLauncher.safeLaunch("*/*") },
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(id = R.string.player_add_external_subtitle),
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
                        iconContentDescription = stringResource(id = R.string.item_selected),
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
            title = stringResource(R.string.player_subtitle_delay),
            delay = currentPlayState.subTitleDelay,
            onConform = { subtitleTrackDialogCallback.onSubtitleDelayChanged(it) },
            onDismiss = { onDialogVisibilityChanged.onSubtitleSettingDialog(false) },
        )
    }
}