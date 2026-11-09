package com.skyd.podaura.podnya.capabilities.feeds.model.repository

import com.skyd.podaura.podnya.capabilities.feeds.model.bean.FeedRecommendation
import com.skyd.podaura.podnya.capabilities.feeds.model.bean.FeedSearchContent
import com.skyd.podaura.podnya.capabilities.feeds.model.bean.FeedSearchError
import com.skyd.podaura.podnya.capabilities.feeds.model.bean.FeedSearchEvidence
import com.skyd.podaura.podnya.capabilities.feeds.model.bean.FeedSearchLimits
import com.skyd.podaura.podnya.capabilities.feeds.model.bean.RecommendedFeed
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantException

class FeedSearchResultValidator(private val source: LocalFeedSearchRepository) {
    suspend fun validate(
        choices: List<FeedRecommendation>,
        evidence: Map<String, FeedSearchEvidence>,
    ): FeedSearchContent {
        val invalid = choices.size > FeedSearchLimits.SEARCH_RESULTS || choices.any {
            it.feedId !in evidence || it.reason.isBlank() || it.reason.length > FeedSearchLimits.REASON_CHARS
        }
        if (invalid) throw AssistantException(AssistantError.INVALID_OUTPUT)

        val selected = choices.distinctBy { it.feedId }
        val refreshed = mutableMapOf<String, LocalFeed>()
        selected.groupBy { evidence.getValue(it.feedId).query }.forEach { (query, items) ->
            val urls = items.map { evidence.getValue(it.feedId).feedUrl }
            // Recheck existence and mute filters; use current titles and descriptions.
            source.search(
                query.copy(
                    keywords = emptyList(),
                    withinFeedUrls = urls,
                    limit = urls.size
                )
            ).filter { it.url in urls }
                .forEach { refreshed[it.url] = it }
        }
        val feeds = selected.mapNotNull { choice ->
            val found = evidence.getValue(choice.feedId)
            refreshed[found.feedUrl]?.let {
                RecommendedFeed(
                    found.feed.copy(
                        title = it.title,
                        description = it.description,
                        muted = it.muted
                    ),
                    choice.reason, it.url
                )
            }
        }
        if (selected.isNotEmpty() && feeds.isEmpty()) throw AssistantException(FeedSearchError.RESULTS_CHANGED)

        return FeedSearchContent(feeds, evidence.isNotEmpty(), feeds.size != selected.size)
    }
}
