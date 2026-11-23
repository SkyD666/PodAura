package com.skyd.podaura.api.model.bean.feed

import com.skyd.podaura.api.model.bean.KeywordMatch

/** Filters for subscriptions already saved in the app. An empty URL list matches nothing. */
data class FeedQuery(
    val keywords: List<String> = emptyList(),
    val match: KeywordMatch = KeywordMatch.ALL,
    val muted: Boolean? = null,
    val withinFeedUrls: List<String>? = null,
    val limit: Int = 20,
)
