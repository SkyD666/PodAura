package com.skyd.podaura.nano.capabilities.feeds.model.bean

import com.skyd.podaura.api.model.bean.KeywordMatch
import com.skyd.podaura.api.model.bean.feed.FeedQuery
import com.skyd.podaura.nano.core.AssistantAction
import com.skyd.podaura.nano.core.AssistantFailure
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class FeedSearchQuery(
    val keywords: List<String> = emptyList(),
    val match: KeywordMatch = KeywordMatch.ALL,
    val muted: Boolean? = null,
    val limit: Int = 20,
)

/** A short description sent to the model. IDs are assigned for the current turn only. */
@Serializable
data class AssistantFeed(
    val feedId: String,
    val title: String,
    val description: String,
    val muted: Boolean
)

@Serializable
data class FeedRecommendation(val feedId: String, val reason: String)

/** Keep the real URL on the device, for opening the subscription's article list. */
@Serializable
data class RecommendedFeed(
    val feed: AssistantFeed,
    val reason: String,
    @Transient val feedUrl: String = "",
)

data class FeedSearchEvidence(val feed: AssistantFeed, val feedUrl: String, val query: FeedQuery)

enum class FeedSearchError : AssistantFailure { LOCAL_SEARCH, RESULTS_CHANGED }

sealed interface FeedSearchAction : AssistantAction {
    data class OpenFeed(val feedUrl: String) : FeedSearchAction {
        override fun toString() = "OpenFeed([redacted])"
    }
}

object FeedSearchLimits {
    const val SEARCH_RESULTS = 20
    const val CANDIDATES = 60
    const val DESCRIPTION_CHARS = 400
    const val REASON_CHARS = 120
}
