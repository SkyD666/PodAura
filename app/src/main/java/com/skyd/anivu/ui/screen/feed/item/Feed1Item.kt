package com.skyd.anivu.ui.screen.feed.item

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skyd.anivu.ext.readable
import com.skyd.anivu.model.bean.feed.FeedBean
import com.skyd.anivu.model.bean.feed.FeedViewBean
import com.skyd.anivu.model.preference.appearance.feed.FeedNumberBadgePreference
import com.skyd.anivu.ui.local.LocalFeedNumberBadge
import com.skyd.anivu.ui.local.LocalNavController
import com.skyd.anivu.ui.screen.article.FeedIcon
import com.skyd.anivu.ui.screen.article.openArticleScreen

@Composable
fun Feed1Item(
    data: FeedViewBean,
    visible: Boolean = true,
    selected: Boolean = false,
    inGroup: Boolean = false,
    isEnd: Boolean = false,
    onClick: ((FeedBean) -> Unit)? = null,
    onEdit: ((FeedViewBean) -> Unit)? = null,
) {
    val navController = LocalNavController.current
    val feed = data.feed

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Row(
            modifier = Modifier
                .clip(
                    if (inGroup) {
                        if (isEnd) RoundedCornerShape(0.dp, 0.dp, SHAPE_CORNER_DP, SHAPE_CORNER_DP)
                        else RectangleShape
                    } else {
                        RoundedCornerShape(12.dp)
                    }
                )
                .background(
                    MaterialTheme.colorScheme.secondary.copy(
                        alpha = if (selected) 0.15f else 0.1f
                    )
                )
                .combinedClickable(
                    onLongClick = if (onEdit != null) {
                        { onEdit(data) }
                    } else null,
                    onClick = {
                        if (onClick == null) {
                            openArticleScreen(
                                navController = navController,
                                feedUrls = arrayListOf(feed.url)
                            )
                        } else onClick(feed)
                    },
                )
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .padding(bottom = if (inGroup && isEnd) 6.dp else 0.dp)
        ) {
            FeedIcon(modifier = Modifier.padding(vertical = 3.dp), data = feed, size = 36.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.align(Alignment.CenterVertically)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val title = rememberSaveable(feed.title, feed.nickname) {
                        feed.nickname.orEmpty().ifBlank { feed.title?.readable().orEmpty() }
                    }
                    Text(
                        modifier = Modifier.weight(1f),
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    FeedNumberBadge(data)
                }
                val description = rememberSaveable(feed.customDescription, feed.description) {
                    if (feed.customDescription == null) {
                        feed.description?.readable().orEmpty()
                    } else {
                        feed.customDescription.readable()
                    }
                }
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedNumberBadge(feedView: FeedViewBean) {
    if (feedView.articleCount > 0) {
        val startEndPadding = 3.dp
        val midPadding = 2.dp
        val feedNumberBadge = LocalFeedNumberBadge.current
        var allCountContent: (@Composable RowScope.() -> Unit)? = null
        var unreadCountContent: (@Composable RowScope.() -> Unit)? = null
        val showUnread = feedNumberBadge and FeedNumberBadgePreference.UNREAD != 0
                && feedView.unreadArticleCount > 0
        val showAll = feedNumberBadge and FeedNumberBadgePreference.ALL != 0
        if (showUnread) {
            unreadCountContent = {
                Text(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.error)
                        .padding(
                            start = startEndPadding,
                            end = if (showAll) midPadding else startEndPadding,
                        ),
                    text = feedView.unreadArticleCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onError,
                )
            }
        }
        if (showAll) {
            allCountContent = {
                Text(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(
                            start = if (showUnread) midPadding else startEndPadding,
                            end = startEndPadding,
                        ),
                    text = feedView.articleCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(modifier = Modifier.clip(RoundedCornerShape(20.dp))) {
            unreadCountContent?.invoke(this)
            allCountContent?.invoke(this)
        }
    }
}