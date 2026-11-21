package com.skyd.podaura.podnya.capabilities.feeds.model.bean

import com.skyd.podaura.podnya.capabilities.SubscriptionHistory
import com.skyd.podaura.podnya.capabilities.SubscriptionReference
import com.skyd.podaura.podnya.core.AssistantContent
import com.skyd.podaura.podnya.core.AssistantHistorySnapshot
import kotlinx.serialization.json.Json

/** Only locally checked feeds can be turned into clickable cards. */
data class FeedSearchContent(
    val feeds: List<RecommendedFeed>,
    val hadCandidates: Boolean,
    val resultsChanged: Boolean = false,
) : AssistantContent, SubscriptionHistory {
    override fun subscriptions() = feeds.filter { it.feedUrl.isNotBlank() }.map {
        SubscriptionReference(title = it.feed.title, feedUrl = it.feedUrl)
    }

    override val retainedBytes: Int = feeds.sumOf {
        it.feed.feedId.encodeToByteArray().size + it.feed.title.encodeToByteArray().size +
                it.feed.description.encodeToByteArray().size + it.reason.encodeToByteArray().size +
                it.feedUrl.encodeToByteArray().size + 64
    } + 128

    override fun historySnapshot() = AssistantHistorySnapshot(
        CAPABILITY_ID, if (feeds.isEmpty()) "NO_MATCH" else "RESULTS", Json.encodeToString(feeds)
    )

    companion object {
        const val CAPABILITY_ID = "feeds"
    }
}
