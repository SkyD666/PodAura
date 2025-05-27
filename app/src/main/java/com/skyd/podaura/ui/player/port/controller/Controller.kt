package com.skyd.podaura.ui.player.port.controller

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Shuffle
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.skyd.podaura.ui.component.shape.CurlyCornerShape
import com.skyd.podaura.ui.player.LoopMode
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.PlayStateCallback
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.loop_playlist_mode
import podaura.shared.generated.resources.pause
import podaura.shared.generated.resources.play
import podaura.shared.generated.resources.shuffle_playlist
import podaura.shared.generated.resources.skip_next
import podaura.shared.generated.resources.skip_previous


@Composable
internal fun Controller(
    playState: PlayState,
    playStateCallback: PlayStateCallback,
    modifier: Modifier = Modifier,
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
        // Shuffle button
        SmallerCircleToggleButton(
            enabled = playState.mediaLoaded,
            checked = playState.shuffle,
            onCheckedChange = playStateCallback.onShuffle,
            imageVector = Icons.Outlined.Shuffle,
            contentDescription = stringResource(Res.string.shuffle_playlist),
            iconSize = 25.dp,
        )
        SmallerCircleButton(
            imageVector = Icons.Outlined.SkipPrevious,
            contentDescription = stringResource(Res.string.skip_previous),
            enabled = !playState.playlistFirst,
            onClick = playStateCallback.onPreviousMedia,
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .size(78.dp)
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
        SmallerCircleButton(
            imageVector = Icons.Outlined.SkipNext,
            contentDescription = stringResource(Res.string.skip_next),
            enabled = !playState.playlistLast,
            onClick = playStateCallback.onNextMedia,
        )
        // Loop button
        SmallerCircleToggleButton(
            enabled = playState.mediaLoaded,
            checked = playState.loop != LoopMode.None,
            onCheckedChange = { playStateCallback.onCycleLoop() },
            imageVector = when (playState.loop) {
                LoopMode.LoopPlaylist, LoopMode.None -> Icons.Outlined.Repeat
                LoopMode.LoopFile -> Icons.Outlined.RepeatOne
            },
            contentDescription = stringResource(Res.string.loop_playlist_mode),
            iconSize = 25.dp,
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
    iconSize: Dp = 36.dp,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconButton(
        modifier = Modifier.size(45.dp),
        onClick = onClick,
        enabled = enabled,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
        )
    }
}