package com.skyd.podaura.ui.screen.calendar.daylist.item

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTooltipState
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skyd.compone.component.ComponeTooltipBox
import com.skyd.compone.component.navigation.LocalNavBackStack
import com.skyd.podaura.ext.readable
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.ui.component.PodAuraImage
import com.skyd.podaura.ui.player.jumper.ArticlePlaylistSource
import com.skyd.podaura.ui.player.jumper.PlayDataMode
import com.skyd.podaura.ui.player.jumper.PlayerJumper
import com.skyd.podaura.ui.player.jumper.rememberPlayerJumper
import com.skyd.podaura.ui.screen.article.enclosure.EnclosureBottomSheet
import com.skyd.podaura.ui.screen.article.enclosure.getEnclosuresList
import com.skyd.podaura.ui.screen.feed.FeedIcon
import com.skyd.podaura.ui.screen.read.ReadRoute
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.play

@Composable
fun ArticleItem(
    articleWithFeed: ArticleWithFeed,
    day: Long,
    playerJumper: PlayerJumper = rememberPlayerJumper(),
) {
    val navBackStack = LocalNavBackStack.current
    val articleWithEnclosure = articleWithFeed.articleWithEnclosure
    val article = articleWithEnclosure.article
    val mediaEnclosures = remember(articleWithEnclosure.enclosures) {
        articleWithEnclosure.enclosures.filter { it.isMedia }
    }
    var openEnclosureBottomSheet by rememberSaveable(article.articleId) { mutableStateOf(false) }

    fun playMedia(url: String) {
        playerJumper.jump(
            PlayDataMode.ArticleList(
                articleId = article.articleId,
                url = url,
                playlistSource = ArticlePlaylistSource.CalendarDay(day),
            )
        )
    }

    val feedName = articleWithFeed.feed.nickname.orEmpty().ifBlank {
        articleWithFeed.feed.title.orEmpty()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                navBackStack.add(
                    ReadRoute(
                        articleId = article.articleId,
                        playlistSource = ArticlePlaylistSource.CalendarDay(day),
                    )
                )
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        var imageLoadError by rememberSaveable(article) { mutableStateOf(false) }
        val cover = articleWithEnclosure.media?.image.orEmpty().ifBlank { article.image }
        val size = 50.dp
        val shape = RoundedCornerShape(6.dp)
        Box(modifier = Modifier.size(size).clip(shape)) {
            if (cover.isNullOrBlank() || imageLoadError) {
                FeedIcon(data = articleWithFeed.feed, size = size, shape = shape)
            } else {
                PodAuraImage(
                    model = cover,
                    modifier = Modifier.matchParentSize(),
                    onError = { imageLoadError = true },
                    contentScale = ContentScale.Crop,
                )
            }
            if (mediaEnclosures.isNotEmpty()) {
                val playLabel = stringResource(Res.string.play)
                ComponeTooltipBox(
                    modifier = Modifier.size(size),
                    tooltip = { PlainTooltip { Text(playLabel) } },
                    state = rememberTooltipState(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable(role = Role.Button) {
                                val enclosure = mediaEnclosures.singleOrNull()
                                if (enclosure == null) {
                                    openEnclosureBottomSheet = true
                                } else {
                                    playMedia(enclosure.url)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            modifier = Modifier.size(28.dp),
                            tint = Color.White,
                            contentDescription = playLabel,
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        val colorAlpha = if (article.isRead) 0.5f else 1f
        CompositionLocalProvider(
            LocalContentColor provides LocalContentColor.current.copy(alpha = colorAlpha)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feedName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = article.title?.readable().orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
    if (openEnclosureBottomSheet) {
        EnclosureBottomSheet(
            onDismissRequest = { openEnclosureBottomSheet = false },
            dataList = remember(articleWithEnclosure) { getEnclosuresList(articleWithEnclosure) },
            article = articleWithFeed,
            onPlay = ::playMedia,
        )
    }
}
