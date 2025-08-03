package com.skyd.podaura.ui.player.port.controller

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Shuffle
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
import com.skyd.podaura.ui.player.LoopMode
import com.skyd.podaura.ui.player.component.ControllerIconButton
import com.skyd.podaura.ui.player.component.ControllerIconToggleButton
import com.skyd.podaura.ui.player.component.ControllerTextButton
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.PlayStateCallback
import com.skyd.podaura.ui.player.component.state.dialog.OnDialogVisibilityChanged
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.fullscreen
import podaura.shared.generated.resources.loop_playlist_mode
import podaura.shared.generated.resources.playlist
import podaura.shared.generated.resources.shuffle_playlist
import java.util.Locale


@Composable
internal fun SmallController(
    playState: PlayState,
    playStateCallback: PlayStateCallback,
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
        // Shuffle button
        ControllerIconToggleButton(
            enabled = playState.mediaLoaded,
            checked = playState.shuffle,
            onCheckedChange = playStateCallback.onShuffle,
            imageVector = Icons.Outlined.Shuffle,
            contentDescription = stringResource(Res.string.shuffle_playlist),
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
        // Loop button
        ControllerIconToggleButton(
            enabled = playState.mediaLoaded,
            checked = playState.loop != LoopMode.None,
            onCheckedChange = { playStateCallback.onCycleLoop() },
            imageVector = when (playState.loop) {
                LoopMode.LoopPlaylist, LoopMode.None -> Icons.Outlined.Repeat
                LoopMode.LoopFile -> Icons.Outlined.RepeatOne
            },
            contentDescription = stringResource(Res.string.loop_playlist_mode),
        )
        ComponeIconButton(
            onClick = { context.activity.landOrientation() },
            imageVector = Icons.Outlined.Fullscreen,
            contentDescription = stringResource(Res.string.fullscreen),
        )
    }
}