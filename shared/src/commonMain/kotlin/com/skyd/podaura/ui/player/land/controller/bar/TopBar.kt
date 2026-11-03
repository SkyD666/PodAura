package com.skyd.podaura.ui.player.land.controller.bar

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.skyd.podaura.ui.player.component.ControllerBarGray
import com.skyd.podaura.ui.player.component.ControllerIconButton
import com.skyd.podaura.ui.player.component.SleepTimerButton
import com.skyd.podaura.ui.player.component.state.dialog.DialogState
import com.skyd.podaura.ui.player.component.state.dialog.OnDialogVisibilityChanged
import com.skyd.podaura.ui.player.pip.rememberOnEnterPip
import com.skyd.podaura.ui.player.pip.supportPip
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.back
import podaura.shared.generated.resources.more
import podaura.shared.generated.resources.player_audio_track
import podaura.shared.generated.resources.player_picture_in_picture
import podaura.shared.generated.resources.player_subtitle_track


@Composable
internal fun TopBar(
    modifier: Modifier = Modifier,
    title: String,
    enabled: Boolean,
    onMenuInteractionChanged: (Boolean) -> Unit,
    onExitFullscreen: () -> Unit,
    dialogState: DialogState,
    onDialogVisibilityChanged: OnDialogVisibilityChanged,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val notify by rememberUpdatedState(onMenuInteractionChanged)
    LaunchedEffect(menuOpen) { notify(menuOpen) }
    DisposableEffect(Unit) { onDispose { notify(false) } }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(ControllerBarGray, Color.Transparent)
                )
            )
            .windowInsetsPadding(
                WindowInsets.systemBars
                    .union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
            )
            .padding(bottom = 30.dp)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier
                .clip(CircleShape)
                .size(56.dp)
                .clickable(onClick = onExitFullscreen)
                .padding(15.dp),
            imageVector = Icons.AutoMirrored.Outlined.ArrowBackIos,
            contentDescription = stringResource(Res.string.back),
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            modifier = Modifier
                .weight(1f)
                .basicMarquee(iterations = Int.MAX_VALUE),
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            maxLines = 1,
        )
        SleepTimerButton(
            sleepTimerState = dialogState.sleepTimerState,
            onClick = { onDialogVisibilityChanged.onSleepTimerDialog(true) },
        )
        if (supportPip) {
            val onEnterPip = rememberOnEnterPip()
            Spacer(modifier = Modifier.width(3.dp))
            ControllerIconButton(
                modifier = Modifier.padding(2.dp),
                enabled = onEnterPip.canEnter,
                onClick = onEnterPip::enter,
                imageVector = Icons.Outlined.PictureInPictureAlt,
                contentDescription = stringResource(Res.string.player_picture_in_picture),
            )
        }
        Box {
            ControllerIconButton(
                onClick = { menuOpen = true },
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = stringResource(Res.string.more),
            )
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.player_audio_track)) },
                    leadingIcon = { Icon(Icons.Rounded.MusicNote, contentDescription = null) },
                    enabled = enabled,
                    onClick = {
                        menuOpen = false
                        onDialogVisibilityChanged.onAudioTrackDialog(true)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.player_subtitle_track)) },
                    leadingIcon = { Icon(Icons.Rounded.ClosedCaption, contentDescription = null) },
                    enabled = enabled,
                    onClick = {
                        menuOpen = false
                        onDialogVisibilityChanged.onSubtitleTrackDialog(true)
                    },
                )
            }
        }
    }
}
