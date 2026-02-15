package com.skyd.podaura.ui.screen.calendar.daylist.item

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.EventListener
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import com.skyd.compone.local.LocalNavController
import com.skyd.podaura.ext.readable
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.ui.component.PodAuraImage
import com.skyd.podaura.ui.component.rememberPodAuraImageLoader
import com.skyd.podaura.ui.screen.feed.FeedIcon
import com.skyd.podaura.ui.screen.read.ReadRoute

@Composable
fun ArticleItem(articleWithFeed: ArticleWithFeed) {
    val navController = LocalNavController.current
    val articleWithEnclosure = articleWithFeed.articleWithEnclosure
    val article = articleWithEnclosure.article
    val feedName = articleWithFeed.feed.nickname.orEmpty().ifBlank {
        articleWithFeed.feed.title.orEmpty()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate(ReadRoute(articleId = article.articleId)) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        var imageLoadError by rememberSaveable(article) { mutableStateOf(false) }
        val cover = articleWithEnclosure.media?.image.orEmpty().ifBlank { article.image }
        val size = 50.dp
        val shape = RoundedCornerShape(6.dp)
        if (cover.isNullOrBlank() || imageLoadError) {
            FeedIcon(data = articleWithFeed.feed, size = size, shape = shape)
        } else {
            PodAuraImage(
                model = cover,
                modifier = Modifier
                    .size(size)
                    .clip(shape),
                imageLoader = rememberPodAuraImageLoader(listener = object : EventListener() {
                    override fun onError(request: ImageRequest, result: ErrorResult) {
                        imageLoadError = true
                    }
                }),
                contentScale = ContentScale.Crop,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        val colorAlpha = if (article.isRead) 0.5f else 1f
        CompositionLocalProvider(
            LocalContentColor provides LocalContentColor.current.copy(alpha = colorAlpha)
        ) {
            Column {
                Text(
                    text = feedName,
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = article.title?.readable().orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                )
            }
        }
    }
}