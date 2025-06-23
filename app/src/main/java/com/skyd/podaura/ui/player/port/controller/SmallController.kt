package com.skyd.podaura.ui.player.port.controller

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
import androidx.compose.ui.unit.dp
import com.skyd.compone.component.ComponeIconButton
import com.skyd.podaura.ext.activity
import com.skyd.podaura.ext.landOrientation
import com.skyd.podaura.ui.player.component.ControllerIconButton
import com.skyd.podaura.ui.player.component.ControllerTextButton
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.dialog.OnDialogVisibilityChanged
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.fullscreen
import podaura.shared.generated.resources.player_audio_track
import podaura.shared.generated.resources.player_subtitle_track
import podaura.shared.generated.resources.playlist
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
            contentDescription = stringResource(Res.string.playlist),
        )
        // Audio track button
        ControllerIconButton(
            enabled = playState.mediaLoaded,
            onClick = { onDialogVisibilityChanged.onAudioTrackDialog(true) },
            imageVector = Icons.Outlined.MusicNote,
            contentDescription = stringResource(Res.string.player_audio_track),
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
            contentDescription = stringResource(Res.string.player_subtitle_track),
        )
        ComponeIconButton(
            onClick = { context.activity.landOrientation() },
            imageVector = Icons.Outlined.Fullscreen,
            contentDescription = stringResource(Res.string.fullscreen),
        )
    }
}