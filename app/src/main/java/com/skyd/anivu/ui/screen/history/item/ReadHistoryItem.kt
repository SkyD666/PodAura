package com.skyd.anivu.ui.screen.history.item

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.skyd.anivu.R
import com.skyd.anivu.ext.readable
import com.skyd.anivu.ext.toDateTimeString
import com.skyd.anivu.model.bean.history.ReadHistoryWithArticle
import com.skyd.anivu.ui.component.PodAuraImage
import com.skyd.anivu.ui.local.LocalDeduplicateTitleInDesc
import com.skyd.anivu.ui.local.LocalNavController
import com.skyd.anivu.ui.screen.article.FeedIcon
import com.skyd.anivu.ui.screen.article.navigateToReadScreen
import com.skyd.anivu.ui.screen.article.openArticleScreen

@Composable
fun ReadHistoryItem(
    data: ReadHistoryWithArticle,
    onDelete: (ReadHistoryWithArticle) -> Unit,
) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val articleWithEnclosure = data.article.articleWithEnclosure
    val article = articleWithEnclosure.article

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondary.copy(0.1f))
            .fillMaxWidth()
            .run { if (article.image.isNullOrBlank()) this else height(IntrinsicSize.Max) }
            .clickable { navigateToReadScreen(navController, articleWithEnclosure) },
    ) {
        val title = article.title?.readable().orEmpty()

        Row(modifier = Modifier.height(IntrinsicSize.Max)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 12.dp)
                    .padding(horizontal = 15.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))

                Row {
                    val author = article.author
                    if (!author.isNullOrBlank()) {
                        Text(
                            text = author,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    val lastTime = data.readHistoryBean.lastTime
                    val date = lastTime.toDateTimeString(context = context)
                    if (lastTime > 0 && date.isNotBlank()) {
                        if (!author.isNullOrBlank()) {
                            Text(
                                modifier = Modifier.padding(horizontal = 3.dp),
                                text = "·",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        Text(
                            text = date,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))

                val description = article.description?.readable()?.let { desc ->
                    if (LocalDeduplicateTitleInDesc.current) desc.replace(title, "") else desc
                }?.trim()
                if (!description.isNullOrBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (!article.image.isNullOrBlank()) {
                OutlinedCard(
                    modifier = Modifier
                        .padding(top = 12.dp, end = 12.dp)
                        .align(Alignment.CenterVertically),
                ) {
                    PodAuraImage(
                        modifier = Modifier
                            .width(100.dp)
                            .fillMaxHeight()
                            .heightIn(min = 70.dp, max = 120.dp)
                            .layout { measurable, constraints ->
                                if (constraints.maxHeight == Constraints.Infinity) {
                                    layout(0, 0) {}
                                } else {
                                    val placeable = measurable.measure(constraints)
                                    layout(placeable.width, placeable.height) {
                                        placeable.place(0, 0)
                                    }
                                }
                            },
                        model = articleWithEnclosure.media?.image.orEmpty()
                            .ifBlank { article.image },
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }

        // Bottom row
        Row(
            modifier = Modifier.padding(start = 15.dp, end = 9.dp, top = 3.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val feed = data.article.feed
            Row(
                modifier = Modifier.clickable {
                    openArticleScreen(
                        navController = navController,
                        feedUrls = listOf(feed.url),
                    )
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FeedIcon(
                    data = feed,
                    size = 22.dp
                )
                val feedName = feed.nickname.orEmpty().ifBlank { feed.title.orEmpty() }
                if (feedName.isNotBlank()) {
                    Text(
                        modifier = Modifier.padding(horizontal = 6.dp),
                        text = feedName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onDelete(data) }
                    .padding(6.dp),
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(id = R.string.delete),
            )
        }
    }
}

@Composable
fun ReadHistoryItemPlaceholder() {
    val color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondary.copy(0.1f))
            .fillMaxWidth(),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Max)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 12.dp)
                    .padding(horizontal = 15.dp),
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .fillMaxWidth(0.7f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.3f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                )
                Spacer(modifier = Modifier.height(7.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color)
                )
            }
        }

        // Bottom row
        Row(
            modifier = Modifier.padding(start = 15.dp, end = 9.dp, top = 3.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(color)
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .fillMaxWidth(0.3f)
                    .height(15.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
            )

            Spacer(modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.width(3.dp))
            Icon(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .padding(6.dp),
                imageVector = Icons.Outlined.Delete,
                tint = color,
                contentDescription = null,
            )
        }
    }
}