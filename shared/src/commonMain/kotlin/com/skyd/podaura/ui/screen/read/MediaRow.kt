package com.skyd.podaura.ui.screen.read

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.ComponentRegistry
import coil3.EventListener
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import com.skyd.compone.component.ComponeIconButton
import com.skyd.compone.component.TagText
import com.skyd.fundation.ext.formatElapsedTime
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.model.bean.article.EnclosureBean
import com.skyd.podaura.model.bean.playlist.MediaUrlWithArticleIdBean.Companion.toMediaUrlWithArticleIdBean
import com.skyd.podaura.ui.component.PodAuraImage
import com.skyd.podaura.ui.component.rememberPodAuraImageLoader
import com.skyd.podaura.ui.screen.playlist.addto.AddToPlaylistSheet
import com.skyd.podaura.util.isFreeNetworkAvailable
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.add_to_playlist
import podaura.shared.generated.resources.play
import podaura.shared.generated.resources.read_screen_episode

expect val components: ComponentRegistry.Builder.() -> Unit

@Composable
private fun MediaCover(
    modifier: Modifier = Modifier.Companion,
    cover: String?,
    enclosure: EnclosureBean,
    episode: String? = null,
    duration: Long? = null,
    onClick: () -> Unit,
) {
    var openAddToPlaylistSheet by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.5f)),
    ) {
        Box(
            modifier = Modifier
                .clickable(onClick = onClick)
                .align(Alignment.Center),
        ) {
            var realImage by rememberSaveable(enclosure) {
                mutableStateOf(if (isFreeNetworkAvailable() && enclosure.isVideo) enclosure.url else cover)
            }
            PodAuraImage(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(min = 200.dp),
                imageLoader = rememberPodAuraImageLoader(
                    listener = object : EventListener() {
                        override fun onError(request: ImageRequest, result: ErrorResult) {
                            if (cover != null && realImage != cover) {
                                realImage = cover
                            }
                        }
                    },
                    components = components,
                ),
                model = realImage,
                contentScale = ContentScale.FillHeight,
                colorFilter = ColorFilter.tint(
                    Color.Black.copy(alpha = 0.5f),
                    blendMode = BlendMode.Darken,
                ),
            )
            Icon(
                modifier = Modifier
                    .size(50.dp)
                    .align(Alignment.Center),
                imageVector = Icons.Outlined.PlayCircleOutline,
                contentDescription = stringResource(Res.string.play),
                tint = Color.White,
            )
            ComponeIconButton(
                onClick = { openAddToPlaylistSheet = true },
                modifier = Modifier.align(Alignment.BottomStart),
                tint = Color.White,
                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                contentDescription = stringResource(Res.string.add_to_playlist),
            )
            if (episode != null) {
                RssMediaEpisode(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 10.dp, top = 10.dp),
                    episode = episode,
                )
            }
            if (duration != null) {
                RssMediaDuration(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp, bottom = 10.dp),
                    duration = duration,
                )
            }
        }
    }

    if (openAddToPlaylistSheet) {
        AddToPlaylistSheet(
            onDismissRequest = { openAddToPlaylistSheet = false },
            currentPlaylistId = null,
            selectedMediaList = remember(enclosure) {
                listOf(enclosure.toMediaUrlWithArticleIdBean())
            },
        )
    }
}

@Composable
private fun RssMediaDuration(modifier: Modifier = Modifier, duration: Long) {
    TagText(modifier = modifier, text = duration.formatElapsedTime())
}

@Composable
private fun RssMediaEpisode(modifier: Modifier = Modifier, episode: String) {
    Text(
        modifier = modifier,
        text = stringResource(Res.string.read_screen_episode, episode),
        color = Color.White,
    )
}

@Composable
internal fun MediaRow(articleWithFeed: ArticleWithFeed, onPlay: (String) -> Unit) {
    val articleWithEnclosure = articleWithFeed.articleWithEnclosure
    val enclosures = articleWithEnclosure.enclosures.filter { it.isMedia }
    val cover = articleWithEnclosure.media?.image ?: articleWithFeed.feed.icon
    if (enclosures.size > 1) {
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(enclosures) { item ->
                MediaCover(
                    modifier = Modifier
                        .height(160.dp)
                        .widthIn(min = 200.dp),
                    cover = cover,
                    enclosure = item,
                    onClick = { onPlay(item.url) },
                )
            }
        }
        val episode = articleWithEnclosure.media?.episode
        val duration = articleWithEnclosure.media?.duration
        if (episode != null || duration != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Row {
                if (episode != null) RssMediaEpisode(episode = episode)
                if (duration != null) {
                    Spacer(modifier = Modifier.width(16.dp))
                    RssMediaDuration(duration = duration)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    } else if (enclosures.size == 1) {
        val item = enclosures.first()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 16.dp)
                .padding(horizontal = 16.dp)
        ) {
            MediaCover(
                modifier = Modifier
                    .height(200.dp)
                    .widthIn(max = 350.dp)
                    .align(Alignment.Center),
                cover = cover,
                enclosure = item,
                episode = articleWithEnclosure.media?.episode,
                duration = articleWithEnclosure.media?.duration,
                onClick = { onPlay(item.url) },
            )
        }
    }
}