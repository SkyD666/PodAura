package com.skyd.anivu.ui.screen.history.item

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.EventListener
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.skyd.anivu.R
import com.skyd.anivu.ext.activity
import com.skyd.anivu.ext.isLocalFile
import com.skyd.anivu.ext.toDateTimeString
import com.skyd.anivu.model.bean.history.MediaPlayHistoryWithArticle
import com.skyd.anivu.model.repository.player.PlayDataMode
import com.skyd.anivu.ui.activity.player.PlayActivity
import com.skyd.anivu.ui.component.PodAuraImage
import com.skyd.anivu.ui.component.TagText
import com.skyd.anivu.ui.component.rememberPodAuraImageLoader
import com.skyd.anivu.ui.local.LocalMediaShowThumbnail
import com.skyd.anivu.ui.local.LocalNavController
import com.skyd.anivu.ui.mpv.isFdFileExists
import com.skyd.anivu.ui.mpv.land.controller.bar.toDurationString
import com.skyd.anivu.ui.screen.read.ReadRoute
import java.io.File

@Composable
fun MediaPlayHistoryItem(
    data: MediaPlayHistoryWithArticle,
    onDelete: (MediaPlayHistoryWithArticle) -> Unit,
) {
    val context = LocalContext.current
    val articleWithEnclosure = data.article?.articleWithEnclosure
    val path = data.mediaPlayHistoryBean.path
    val fileName = rememberSaveable(data) {
        articleWithEnclosure?.article?.title ?: path.substringAfterLast("/")
    }
    val isLocal = path.isLocalFile()
    val mediaExists = remember(path) {
        !isLocal || (path.startsWith("fd://") && isFdFileExists(path) || File(path).exists())
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondary.copy(0.1f))
            .clickable {
                if (!mediaExists) {
                    return@clickable
                }
                val articleId = articleWithEnclosure?.article?.articleId
                if (isLocal || articleId == null) {
                    PlayActivity.playMediaList(
                        activity = context.activity,
                        startMediaPath = path,
                        mediaList = listOf(
                            PlayDataMode.MediaLibraryList.PlayMediaListItem(
                                path = path,
                                articleId = articleId,
                                title = null,
                                thumbnail = null,
                            )
                        ),
                    )
                } else {
                    PlayActivity.playArticleList(
                        activity = context.activity,
                        articleId = articleId,
                        url = path,
                    )
                }
            }
            .padding(vertical = 6.dp)
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val feed = data.article?.feed
        val image = articleWithEnclosure?.media?.image ?: feed?.icon ?: path
        var showThumbnail by remember(image) { mutableStateOf(true) }
        if (showThumbnail && LocalMediaShowThumbnail.current) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .size(50.dp),
                contentAlignment = Alignment.Center,
            ) {
                PodAuraImage(
                    modifier = Modifier.fillMaxSize(),
                    model = remember(image) {
                        ImageRequest.Builder(context)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .data(image)
                            .crossfade(true)
                            .build()
                    },
                    imageLoader = rememberPodAuraImageLoader(listener = object :
                        EventListener() {
                        override fun onError(request: ImageRequest, result: ErrorResult) {
                            showThumbnail = false
                        }
                    }),
                    contentScale = ContentScale.Crop,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        }
        Column {
            Text(
                text = fileName,
                modifier = Modifier.padding(top = 6.dp, end = 16.dp),
                maxLines = 3,
                style = MaterialTheme.typography.titleSmall,
            )
            TagRow(isLocal = isLocal, mediaExists = mediaExists)
            Row(
                modifier = Modifier.padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .basicMarquee(),
                ) {
                    val lastPlayPosition = data.mediaPlayHistoryBean.lastPlayPosition
                    Text(
                        text = stringResource(
                            R.string.history_screen_last_seen,
                            (lastPlayPosition / 1000).toDurationString(),
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.widthIn(min = 12.dp))
                    val lastTime = data.mediaPlayHistoryBean.lastTime
                    if (lastTime > 0) {
                        Text(
                            text = remember(lastTime) { lastTime.toDateTimeString(context = context) },
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                        )
                    }
                }
                ActionIconButton(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(id = R.string.delete),
                    onClick = { onDelete(data) },
                )
                if (articleWithEnclosure != null) {
                    val navController = LocalNavController.current
                    ActionIconButton(
                        imageVector = Icons.AutoMirrored.Outlined.Article,
                        contentDescription = stringResource(id = R.string.read_screen_name),
                        onClick = { navController.navigate(ReadRoute(articleId = articleWithEnclosure.article.articleId)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TagRow(isLocal: Boolean, mediaExists: Boolean) {
    val tagRow: List<@Composable RowScope.() -> Unit> = buildList {
        if (isLocal) {
            add {
                TagText(text = stringResource(R.string.history_screen_local_file))
            }
        }
        if (!mediaExists) {
            add {
                TagText(
                    text = stringResource(R.string.media_not_exists),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
    if (tagRow.isNotEmpty()) {
        Row(
            modifier = Modifier.padding(top = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            tagRow.forEach { it() }
        }
    }
}

@Composable
private fun ActionIconButton(
    imageVector: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
) {
    Icon(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(3.dp),
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = LocalContentColor.current.copy(alpha = 0.75f)
    )
}

@Composable
fun MediaPlayItemPlaceholder() {
    val color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondary.copy(0.1f))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(color)
                .size(50.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxSize()
                    .height(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color)
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color)
                )
                Icon(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .padding(6.dp),
                    imageVector = Icons.Outlined.Delete,
                    tint = color,
                    contentDescription = stringResource(id = R.string.delete),
                )
            }
        }
    }
}