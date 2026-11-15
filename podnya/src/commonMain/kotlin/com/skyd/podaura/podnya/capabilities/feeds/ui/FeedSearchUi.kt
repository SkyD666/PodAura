package com.skyd.podaura.podnya.capabilities.feeds.ui

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.skyd.podaura.podnya.capabilities.feeds.model.bean.FeedSearchAction
import com.skyd.podaura.podnya.capabilities.feeds.model.bean.FeedSearchContent
import com.skyd.podaura.podnya.capabilities.feeds.model.bean.FeedSearchError
import com.skyd.podaura.podnya.capabilities.feeds.model.repository.LocalFeedSearchRepository
import com.skyd.podaura.podnya.core.AssistantAction
import com.skyd.podaura.podnya.core.AssistantContent
import com.skyd.podaura.podnya.core.AssistantFailure
import com.skyd.podaura.podnya.ui.AssistantCapabilityUi
import com.skyd.podnya.resources.Res
import com.skyd.podnya.resources.podnya_error_feed_search
import com.skyd.podnya.resources.podnya_error_feeds_changed
import com.skyd.podnya.resources.podnya_feed_results
import com.skyd.podnya.resources.podnya_missing_feeds
import com.skyd.podnya.resources.podnya_no_matching_feeds
import com.skyd.podnya.resources.podnya_no_suitable_recommendations
import com.skyd.podnya.resources.podnya_ok
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

fun LazyListScope.feedSearchContent(
    turnId: String,
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
    override fun itemKeys(
        turnId: String,
        content: AssistantContent,
        showFallbackText: Boolean
    ): List<String> = buildList {
        val value = content as FeedSearchContent
        add("$turnId-feed-summary")
        if (value.resultsChanged) {
            add("$turnId-feeds-changed")
        }
        value.feeds.forEach { add("$turnId-feed-${it.feed.feedId}") }
    }

    override fun LazyListScope.render(
        turnId: String, content: AssistantContent, showFallbackText: Boolean,
        formatDate: (Long) -> String, onAction: (AssistantAction) -> Unit,
    ) = feedSearchContent(turnId, content as FeedSearchContent, onAction)

    @Composable
    override fun failureText(error: AssistantFailure): String? =
        (error as? FeedSearchError)?.let { feedSearchErrorText(it) }
}

/** Opening an old card checks local existence only; viewing history never re-runs a search. */
@Composable
fun rememberFeedHistoryOpener(
    onOpen: (String) -> Unit,
    repository: LocalFeedSearchRepository = koinInject()
): (String) -> Unit {
    val scope = rememberCoroutineScope()
    val currentOpen by rememberUpdatedState(onOpen)
    var missing by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    if (missing || failed) AlertDialog(
        onDismissRequest = { missing = false; failed = false },
        text = {
            Text(
                if (missing) stringResource(Res.string.podnya_missing_feeds)
                else feedSearchErrorText(FeedSearchError.LOCAL_SEARCH)
            )
        },
        confirmButton = {
            TextButton(onClick = { missing = false; failed = false }) {
                Text(stringResource(Res.string.podnya_ok))
            }
        })
    return remember(repository, scope) {
        { id ->
            scope.launch {
                try {
                    if (repository.exists(id)) currentOpen(id) else missing = true
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    failed = true
                }
            }
        }
    }
}
