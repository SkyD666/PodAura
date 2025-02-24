package com.skyd.anivu.ui.mpv.port.controller

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skyd.anivu.R
import com.skyd.anivu.ext.activity
import com.skyd.anivu.ext.landOrientation
import com.skyd.anivu.ui.component.PodAuraIconButton
import com.skyd.anivu.ui.mpv.component.ControllerIconButton
import com.skyd.anivu.ui.mpv.component.ControllerTextButton
import com.skyd.anivu.ui.mpv.component.state.PlayState
import com.skyd.anivu.ui.mpv.component.state.dialog.OnDialogVisibilityChanged
import java.util.Locale


@Composable
internal fun SmallController(
    playState: PlayState,
    onDialogVisibilityChanged: OnDialogVisibilityChanged,
    onOpenPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        // Playlist button
        ControllerIconButton(
            onClick = onOpenPlaylist,
            imageVector = Icons.AutoMirrored.Outlined.PlaylistPlay,
            contentDescription = stringResource(R.string.playlist),
        )
        // Audio track button
        ControllerIconButton(
            enabled = playState.mediaLoaded,
            onClick = { onDialogVisibilityChanged.onAudioTrackDialog(true) },
            imageVector = Icons.Outlined.MusicNote,
            contentDescription = stringResource(R.string.player_audio_track),
        )
        // Speed button
        ControllerTextButton(
            text = "${String.format(Locale.getDefault(), "%.2f", playState.speed)}x",
            enabled = playState.mediaLoaded,
            colors = ButtonDefaults.textButtonColors().copy(
                contentColor = LocalContentColor.current,
                disabledContentColor = LocalContentColor.current.copy(alpha = 0.6f),
            ),
            onClick = { onDialogVisibilityChanged.onSpeedDialog(true) },
        )
        // Subtitle track button
        ControllerIconButton(
            enabled = playState.mediaLoaded,
            onClick = { onDialogVisibilityChanged.onSubtitleTrackDialog(true) },
            imageVector = Icons.Outlined.ClosedCaption,
            contentDescription = stringResource(R.string.player_subtitle_track),
        )
        PodAuraIconButton(
            onClick = { context.activity.landOrientation() },
            imageVector = Icons.Outlined.Fullscreen,
            contentDescription = stringResource(R.string.fullscreen),
        )
    }
}