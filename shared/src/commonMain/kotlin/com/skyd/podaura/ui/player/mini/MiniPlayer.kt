package com.skyd.podaura.ui.player.mini

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skyd.podaura.ext.isCompact
import com.skyd.podaura.ui.component.PodAuraImage
import com.skyd.podaura.ui.local.LocalWindowSizeClass
import com.skyd.podaura.ui.player.PlayerCommand
import com.skyd.podaura.ui.player.withoutHotPlayerValues
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.player.service.PlayerState
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.close
import podaura.shared.generated.resources.pause
import podaura.shared.generated.resources.play
import podaura.shared.generated.resources.skip_next
import podaura.shared.generated.resources.skip_previous
import podaura.shared.generated.resources.unknown

@Composable
fun MiniPlayer(
    coordinator: PlayerCoordinator?,
    onOpenPlayer: () -> Unit,
    onClosePlayer: () -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    windowInsets: WindowInsets = WindowInsets(),
) {
    var retainedState by remember { mutableStateOf<PlayerState?>(null) }
    val liveState = coordinator?.playerState?.let { source ->
        remember(source) {
            source.withoutHotPlayerValues()
        }.collectAsStateWithLifecycle(
            initialValue = source.value.copy(
                position = 0L,
                buffer = 0,
                zoom = 1f,
                offsetX = 0f,
                offsetY = 0f,
                rotate = 0f,
            )
        ).value
    }
    val sessionAvailable = liveState?.currentMedia != null
    val sessionVisible = isMiniPlayerVisible(
        sessionAvailable = sessionAvailable,
        hostVisible = visible,
    )

    SideEffect {
        if (sessionAvailable) {
            retainedState = liveState
        }
    }

    AnimatedVisibility(
        visible = sessionVisible,
        modifier = modifier.fillMaxWidth(),
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        DisposableEffect(Unit) {
            onDispose { retainedState = null }
        }
        val state = liveState?.takeIf { it.currentMedia != null }
            ?: retainedState
            ?: return@AnimatedVisibility
        val currentMedia = state.currentMedia ?: return@AnimatedVisibility

        val unknown = stringResource(Res.string.unknown)
        val title = currentMedia.title
            .ifBlank { state.mediaTitle.orEmpty() }
            .ifBlank { unknown }
        val artist = currentMedia.artist.orEmpty()
            .ifBlank { state.artist.orEmpty() }
            .ifBlank { state.album.orEmpty() }
            .ifBlank { unknown }
        val isPlaying = state.mediaStarted && !state.paused
        val isCompact = LocalWindowSizeClass.current.isCompact

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
        ) {
            Column(modifier = Modifier.windowInsetsPadding(windowInsets)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clickable(enabled = sessionVisible, onClick = onOpenPlayer)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        PodAuraImage(
                            model = currentMedia.thumbnailAny ?: state.mediaThumbnail,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }

                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (!isCompact) {
                        IconButton(
                            enabled = sessionVisible && !state.playlistFirst,
                            onClick = { coordinator?.onCommand(PlayerCommand.PreviousMedia) },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SkipPrevious,
                                contentDescription = stringResource(Res.string.skip_previous),
                            )
                        }
                    }

                    IconButton(
                        enabled = sessionVisible,
                        onClick = { coordinator?.onCommand(PlayerCommand.PlayOrPause) },
                    ) {
                        Icon(
                            imageVector = if (isPlaying) {
                                Icons.Filled.Pause
                            } else {
                                Icons.Filled.PlayArrow
                            },
                            contentDescription = stringResource(
                                if (isPlaying) Res.string.pause else Res.string.play
                            ),
                        )
                    }

                    IconButton(
                        enabled = sessionVisible && !state.playlistLast,
                        onClick = { coordinator?.onCommand(PlayerCommand.NextMedia) },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SkipNext,
                            contentDescription = stringResource(Res.string.skip_next),
                        )
                    }

                    IconButton(enabled = sessionVisible, onClick = onClosePlayer) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(Res.string.close),
                        )
                    }
                }

                MiniPlayerProgressIndicator(coordinator = coordinator, fallbackState = state)
            }
        }
    }
}

@Composable
private fun MiniPlayerProgressIndicator(
    coordinator: PlayerCoordinator?,
    fallbackState: PlayerState,
) {
    val liveState = coordinator?.playerState?.collectAsStateWithLifecycle()?.value
    val position = liveState?.position ?: fallbackState.position
    val duration = liveState?.duration ?: fallbackState.duration
    val progress by animateFloatAsState(
        targetValue = miniPlayerProgress(position, duration),
        label = "miniPlayerProgress",
    )
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth().height(3.dp),
    )
}

internal fun miniPlayerProgress(position: Long, duration: Long): Float {
    if (duration <= 0L) return 0f
    return (position.toDouble() / duration).toFloat().coerceIn(0f, 1f)
}

internal fun isMiniPlayerVisible(sessionAvailable: Boolean, hostVisible: Boolean): Boolean =
    sessionAvailable && hostVisible
