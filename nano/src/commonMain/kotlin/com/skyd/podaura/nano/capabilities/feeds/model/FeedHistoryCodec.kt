package com.skyd.podaura.nano.capabilities.feeds.model

import com.skyd.podaura.nano.capabilities.feeds.model.bean.AssistantFeed
import com.skyd.podaura.nano.capabilities.feeds.model.bean.FeedSearchContent
import com.skyd.podaura.nano.capabilities.feeds.model.bean.FeedSearchError
import com.skyd.podaura.nano.capabilities.feeds.model.bean.RecommendedFeed
import com.skyd.podaura.nano.core.AssistantContent
import com.skyd.podaura.nano.core.AssistantFailure
import com.skyd.podaura.nano.core.AssistantHistoryCodec
import com.skyd.podaura.nano.core.CapabilityRecord
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object FeedHistoryCodec : AssistantHistoryCodec {
    override val capabilityId = FeedSearchContent.CAPABILITY_ID
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class Card(val feed: AssistantFeed, val reason: String, val feedUrl: String)

    @Serializable
    private data class Content(
        val feeds: List<Card>,
        val hadCandidates: Boolean,
        val resultsChanged: Boolean,
    )

    override fun encodeFailure(error: AssistantFailure): String? =
        (error as? FeedSearchError)?.let { "$capabilityId:${it.name}" }

    override fun decodeFailure(value: String): AssistantFailure? =
        if (value.startsWith("$capabilityId:")) {
            runCatching { FeedSearchError.valueOf(value.substringAfter(':')) }.getOrNull()
        } else null

    override fun encodeContent(content: AssistantContent): CapabilityRecord? {
        if (content !is FeedSearchContent) return null
        return CapabilityRecord(
            capabilityId = capabilityId,
            version = 1,
            payload = json.encodeToString(
                Content(
                    feeds = content.feeds.map {
                        Card(feed = it.feed, reason = it.reason, feedUrl = it.feedUrl)
                    },
                    hadCandidates = content.hadCandidates,
                    resultsChanged = content.resultsChanged,
                )
            ),
            fallback = content.feeds.joinToString("\n") { "${it.feed.title}\n${it.reason}" },
        )
    }

    override fun decodeContent(record: CapabilityRecord): AssistantContent {
        require(record.version == 1)
        val value = json.decodeFromString<Content>(record.payload)
        return FeedSearchContent(
            feeds = value.feeds.map {
                RecommendedFeed(feed = it.feed, reason = it.reason, feedUrl = it.feedUrl)
            },
            hadCandidates = value.hadCandidates,
            resultsChanged = value.resultsChanged,
        )
    }
}
