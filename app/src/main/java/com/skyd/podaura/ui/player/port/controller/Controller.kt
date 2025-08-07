package com.skyd.podaura.ui.player.port.controller

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.IconToggleButtonColors
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skyd.compone.ext.thenIf
import com.skyd.podaura.ext.mirror
import com.skyd.podaura.model.preference.player.PlayerForwardSecondsPreference
import com.skyd.podaura.model.preference.player.PlayerReplaySecondsPreference
import com.skyd.podaura.ui.component.LongClickListener
import com.skyd.podaura.ui.component.shape.CurlyCornerShape
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.PlayStateCallback
import com.skyd.podaura.ui.player.component.state.dialog.OnDialogVisibilityChanged
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.pause
import podaura.shared.generated.resources.play
import podaura.shared.generated.resources.player_forward_seconds_content_description
import podaura.shared.generated.resources.player_replay_seconds_content_description
import podaura.shared.generated.resources.skip_next
import podaura.shared.generated.resources.skip_previous
import kotlin.math.abs


@Composable
internal fun Controller(
    playState: PlayState,
    playStateCallback: PlayStateCallback,
    modifier: Modifier = Modifier,
    onDialogVisibilityChanged: OnDialogVisibilityChanged,
) {
    val density = LocalDensity.current
    val animatePlayButtonShapeAmp by animateDpAsState(
        if (playState.isPlaying) 1.6.dp else 0.dp,
        label = "animatePlayButtonShapeAmp",
    )
    val animatePlayButtonShapeCount by animateFloatAsState(
        if (playState.isPlaying) 12f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "animatePlayButtonShapeCount",
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmallerCircleButton(
            imageVector = Icons.Outlined.SkipPrevious,
            contentDescription = stringResource(Res.string.skip_previous),
            enabled = !playState.playlistFirst,
            onClick = playStateCallback.onPreviousMedia,
        )
        // -seconds
        ForwardOrReplayButton(
            seconds = PlayerReplaySecondsPreference.current,
            contentDescription = stringResource(Res.string.player_replay_seconds_content_description),
            enabled = playState.mediaLoaded,
            playState = playState,
            playStateCallback = playStateCallback,
            onLongClick = { onDialogVisibilityChanged.onReplaySecondDialog(true) },
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .size(76.dp)
                .clip(
                    CurlyCornerShape(
                        amp = with(density) { animatePlayButtonShapeAmp.toPx() },
                        count = animatePlayButtonShapeCount,
                    )
                )
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, shape = CircleShape)
                .clickable(onClick = playStateCallback.onPlayStateChanged),
            contentAlignment = Alignment.Center,
        ) {
            if (playState.loading) {
                LoadingIndicator(modifier = Modifier.size(40.dp))
            } else {
                Icon(
                    imageVector = if (playState.isPlaying) Icons.Filled.Pause
                    else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(
                        if (playState.isPlaying) Res.string.pause else Res.string.play
                    ),
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // +seconds
        ForwardOrReplayButton(
            seconds = PlayerForwardSecondsPreference.current,
            contentDescription = stringResource(Res.string.player_forward_seconds_content_description),
            enabled = playState.mediaLoaded,
            playState = playState,
            playStateCallback = playStateCallback,
            onLongClick = { onDialogVisibilityChanged.onForwardSecondDialog(true) },
        )
        SmallerCircleButton(
            imageVector = Icons.Outlined.SkipNext,
            contentDescription = stringResource(Res.string.skip_next),
            enabled = !playState.playlistLast,
            onClick = playStateCallback.onNextMedia,
        )
    }
}

@Composable
private fun ForwardOrReplayButton(
    seconds: Int,
    contentDescription: String?,
    enabled: Boolean,
    playState: PlayState,
    playStateCallback: PlayStateCallback,
    onLongClick: () -> Unit,
) {
    Box(contentAlignment = Alignment.Center) {
        SmallerCircleButton(
            imageVector = Icons.Outlined.Replay,
            contentDescription = contentDescription,
            mirrorIcon = seconds >= 0,
            enabled = enabled,
            onLongClick = onLongClick,
            onClick = { playStateCallback.onSeekTo(playState.position + seconds) },
        )
        Text(
            text = abs(seconds).toString(),
            modifier = Modifier.padding(top = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontSize = if (abs(seconds) >= 100) 6.sp else 9.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SmallerCircleToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    imageVector: ImageVector,
    contentDescription: String?,
    iconSize: Dp = 36.dp,
    enabled: Boolean = true,
    colors: IconToggleButtonColors = IconButtonDefaults.iconToggleButtonColors(
        contentColor = LocalContentColor.current.copy(alpha = 0.4f),
        checkedContentColor = LocalContentColor.current,
    ),
) {
    IconToggleButton(
        modifier = Modifier.size(45.dp),
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = colors,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun SmallerCircleButton(
    imageVector: ImageVector,
    contentDescription: String?,
    iconSize: Dp = 32.dp,
    mirrorIcon: Boolean = false,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    LongClickListener(
        interactionSource = interactionSource,
        onLongClick = onLongClick,
        onClick = onClick,
    )
    IconButton(
        modifier = Modifier.size(46.dp),
        onClick = {},
        enabled = enabled,
        interactionSource = interactionSource,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier
                .size(iconSize)
                .thenIf(mirrorIcon) { mirror() },
        )
    }
}