package com.skyd.podaura.podnya.capabilities.feeds.ui

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.skyd.podaura.podnya.capabilities.feeds.model.bean.FeedSearchAction
import com.skyd.podaura.podnya.capabilities.feeds.model.bean.FeedSearchContent
import com.skyd.podaura.podnya.capabilities.feeds.model.bean.FeedSearchError
import com.skyd.podaura.podnya.core.AssistantAction
import com.skyd.podaura.podnya.core.AssistantContent
import com.skyd.podaura.podnya.core.AssistantFailure
import com.skyd.podaura.podnya.ui.AssistantCapabilityUi
import com.skyd.podnya.resources.Res
import com.skyd.podnya.resources.podnya_error_feed_search
import com.skyd.podnya.resources.podnya_error_feeds_changed
import com.skyd.podnya.resources.podnya_feed_results
import com.skyd.podnya.resources.podnya_no_matching_feeds
import com.skyd.podnya.resources.podnya_no_suitable_recommendations
import org.jetbrains.compose.resources.stringResource

fun LazyListScope.feedSearchContent(
    turnId: Long,
    content: FeedSearchContent,
    onAction: (FeedSearchAction) -> Unit
) {
    item(key = "$turnId-feed-summary") {
        Text(
            stringResource(
                when {
                    content.feeds.isNotEmpty() -> Res.string.podnya_feed_results
                    content.hadCandidates -> Res.string.podnya_no_suitable_recommendations
                    else -> Res.string.podnya_no_matching_feeds
                }, content.feeds.size
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (content.resultsChanged) item(key = "$turnId-feeds-changed") {
        Text(
            stringResource(Res.string.podnya_error_feeds_changed),
            style = MaterialTheme.typography.bodySmall
        )
    }
    content.feeds.forEach { item ->
        item(key = "$turnId-feed-${item.feed.feedId}") {
            FeedRecommendationItem(
                recommendation = item,
                onClick = { onAction(FeedSearchAction.OpenFeed(item.feedUrl)) },
            )
        }
    }
}

@Composable
fun feedSearchErrorText(error: FeedSearchError): String = stringResource(
    when (error) {
        FeedSearchError.LOCAL_SEARCH -> Res.string.podnya_error_feed_search
        FeedSearchError.RESULTS_CHANGED -> Res.string.podnya_error_feeds_changed
    }
)

internal object FeedSearchUi : AssistantCapabilityUi {
    override fun supports(content: AssistantContent) = content is FeedSearchContent
    override fun LazyListScope.render(
        turnId: Long, content: AssistantContent, showFallbackText: Boolean,
        formatDate: (Long) -> String, onAction: (AssistantAction) -> Unit,
    ) = feedSearchContent(turnId, content as FeedSearchContent, onAction)

    @Composable
    override fun failureText(error: AssistantFailure): String? =
        (error as? FeedSearchError)?.let { feedSearchErrorText(it) }
}
