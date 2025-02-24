package com.skyd.anivu.ui.mpv.component.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.EventListener
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import com.skyd.anivu.R
import com.skyd.anivu.base.mvi.getDispatcher
import com.skyd.anivu.ui.component.CircularProgressPlaceholder
import com.skyd.anivu.ui.component.ErrorPlaceholder
import com.skyd.anivu.ui.component.PodAuraImage
import com.skyd.anivu.ui.component.rememberPodAuraImageLoader
import com.skyd.anivu.ui.mpv.land.controller.bar.toDurationString
import com.skyd.anivu.ui.mpv.service.PlaylistBean

@Composable
fun Playlist(
    currentPlay: String?,
    playlist: List<PlaylistBean>,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onPlay: (PlaylistBean) -> Unit,
    viewModel: PlaylistViewModel = hiltViewModel(),
) {
    viewModel.getDispatcher(key1 = playlist, startWith = PlaylistIntent.Init(playlist))
    val uiState by viewModel.viewState.collectAsStateWithLifecycle()

    when (val listState = uiState.listState) {
        is ListState.Failed -> ErrorPlaceholder(listState.msg, contentPadding)
        ListState.Init,
        ListState.Loading -> CircularProgressPlaceholder(contentPadding)

        is ListState.Success -> LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(300.dp),
            contentPadding = contentPadding,
        ) {
            items(listState.playlist, key = { it.path }) { item ->
                PlaylistItem(
                    isPlaying = currentPlay == item.path,
                    data = item,
                    onPlay = onPlay,
                )
            }
        }
    }
}

@Composable
private fun PlaylistItem(
    isPlaying: Boolean,
    data: PlaylistItemBean,
    onPlay: (PlaylistBean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onPlay(data.playlistBean) })
            .padding(horizontal = 20.dp, vertical = 8.dp),
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
                    if (!isPlaying) {
                        Icon(
                            imageVector = Icons.Outlined.MusicNote,
                            contentDescription = null,
                        )
                    }
                }
            } else {
                PodAuraImage(
                    model = data.thumbnailBitmap ?: data.thumbnail ?: data.path,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    imageLoader = rememberPodAuraImageLoader(listener = object : EventListener() {
                        override fun onError(request: ImageRequest, result: ErrorResult) {
                            imageLoadError = true
                        }
                    }),
                )
            }
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.playing),
                        modifier = Modifier.size(32.dp),
                        tint = Color.White,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = data.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            CompositionLocalProvider(
                LocalContentColor provides LocalContentColor.current.copy(alpha = 0.7f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (data.artist != null) {
                        Text(
                            text = data.artist,
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
                    if (data.duration != null) {
                        Text(
                            text = data.duration.toDurationString(),
                            modifier = Modifier.alignByBaseline(),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}