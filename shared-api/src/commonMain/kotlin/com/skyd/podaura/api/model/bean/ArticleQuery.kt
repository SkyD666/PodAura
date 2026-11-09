package com.skyd.podaura.api.model.bean

import kotlinx.serialization.Serializable

/** Which local articles the app allows this search to use. */
@Serializable
sealed interface ArticleSearchScope {
    @Serializable
    data object All : ArticleSearchScope

    /** Allow articles from these feeds OR these article IDs. Both empty means no articles. */
    @Serializable
    data class Restricted(
        val feedUrls: Set<String> = emptySet(),
        val articleIds: Set<String> = emptySet(),
    ) : ArticleSearchScope
}

/** Filters for local search. Dates are Unix timestamps in milliseconds. */
data class ArticleQuery(
    val keywords: List<String> = emptyList(),
    val match: KeywordMatch = KeywordMatch.ALL,
    val publishedFromMillis: Long? = null,
    val publishedBeforeMillis: Long? = null,
    val read: Boolean? = null,
    val favorite: Boolean? = null,
    val withinArticleIds: List<String>? = null,
    val excludeArticleIds: List<String> = emptyList(),
    val limit: Int = 20,
)

