package com.skyd.podaura.ui.screen.playlist.medialist

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.EventListener
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import com.skyd.compone.component.ComponeIconButton
import com.skyd.compone.component.TagText
import com.skyd.compone.ext.thenIf
import com.skyd.podaura.ext.isLocalFileExists
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.ui.component.PodAuraImage
import com.skyd.podaura.ui.component.rememberPodAuraImageLoader
import com.skyd.podaura.ui.player.land.controller.bar.toDurationString
import com.skyd.podaura.util.coil.localmedia.LocalMediaFetcher
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.history_screen_local_file
import podaura.shared.generated.resources.media_not_exists
import podaura.shared.generated.resources.playing

@Composable
fun PlaylistMediaItem(
    playing: Boolean,
    selected: Boolean,
    dragIconModifier: Modifier = Modifier,
    draggable: Boolean = false,
    data: PlaylistMediaWithArticleBean,
    onClick: (PlaylistMediaWithArticleBean) -> Unit,
    onLongClick: (PlaylistMediaWithArticleBean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .thenIf(selected) { background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)) }
            .combinedClickable(onLongClick = { onLongClick(data) }, onClick = { onClick(data) })
            .padding(vertical = 8.dp)
            .padding(start = 20.dp, end = if (draggable) 6.dp else 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(4.dp)),
        ) {
            var imageLoadError by rememberSaveable(data) { mutableStateOf(false) }
            if (imageLoadError) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!playing) {
                        Icon(
                            imageVector = Icons.Outlined.MusicNote,
                            contentDescription = null,
                        )
                    }
                }
            } else {
                val thumbnail = remember(data) { data.thumbnailAny }
                PodAuraImage(
                    model = thumbnail,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    imageLoader = rememberPodAuraImageLoader(
                        listener = object : EventListener() {
                            override fun onError(request: ImageRequest, result: ErrorResult) {
                                imageLoadError = true
                            }
                        },
                        components = { add(LocalMediaFetcher.Factory()) },
                    ),
                )
            }
            if (playing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = stringResource(Res.string.playing),
                        modifier = Modifier.size(32.dp),
                        tint = Color.White,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = data.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val url = data.playlistMediaBean.url
            val isLocalFile = data.playlistMediaBean.isLocalFile
            val mediaExists = remember(url) { !isLocalFile || url.isLocalFileExists() }
            TagRow(
                isLocal = data.playlistMediaBean.isLocalFile,
                mediaExists = mediaExists
            )
            CompositionLocalProvider(
                LocalContentColor provides LocalContentColor.current.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    data.artist?.let { artist ->
                        Text(
                            text = artist,
                            modifier = Modifier.alignByBaseline(),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (data.duration != null) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(LocalContentColor.current)
                            )
                        }
                    }
                    data.duration?.let { duration ->
                        Text(
                            text = (duration / 1000).toDurationString(),
                            modifier = Modifier.alignByBaseline(),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        if (selected) {
            Spacer(modifier = Modifier.width(12.dp))
            Checkbox(
                checked = true,
                onCheckedChange = null,
            )
        }
        if (draggable) {
            ComponeIconButton(
                onClick = {},
                imageVector = Icons.Rounded.DragHandle,
                modifier = dragIconModifier,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun TagRow(isLocal: Boolean, mediaExists: Boolean) {
    val tagRow: List<@Composable RowScope.() -> Unit> = buildList {
        if (isLocal) {
            add {
                TagText(text = stringResource(Res.string.history_screen_local_file))
            }
        }
        if (!mediaExists) {
            add {
                TagText(
                    text = stringResource(Res.string.media_not_exists),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
    if (tagRow.isNotEmpty()) {
        Row(
            modifier = Modifier.padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            tagRow.forEach { it() }
        }
    }
}

@Composable
fun PlaylistMediaItemPlaceholder() {
    val color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(4.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.3f)
                    .height(15.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}