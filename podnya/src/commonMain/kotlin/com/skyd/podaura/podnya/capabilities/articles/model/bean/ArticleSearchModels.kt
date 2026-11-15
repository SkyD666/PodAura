package com.skyd.podaura.podnya.capabilities.articles.model.bean

import com.skyd.podaura.api.model.bean.ArticleQuery
import com.skyd.podaura.api.model.bean.KeywordMatch
import com.skyd.podaura.podnya.core.AssistantAction
import com.skyd.podaura.podnya.core.AssistantAnswerKind
import com.skyd.podaura.podnya.core.AssistantFailure
import com.skyd.podaura.podnya.core.AssistantProgress
import kotlinx.serialization.Serializable

/** Search filters from the model. Null values leave the related filter off. */
@Serializable
data class ArticleSearchQuery(
    val keywords: List<String> = emptyList(),
    val match: KeywordMatch = KeywordMatch.ALL,
    /** Start date in YYYY-MM-DD form; include this day. */
    val publishedFrom: String? = null,
    /** End date in YYYY-MM-DD form; do not include this day. */
    val publishedBefore: String? = null,
    val read: Boolean? = null,
    val favorite: Boolean? = null,
    /** Null adds no ID filter; an empty list means no articles. */
    val withinArticleIds: List<String>? = null,
    val excludeArticleIds: List<String> = emptyList(),
)

/** Articles found by one search; hasMore means more matches exist beyond this page. */
@Serializable
data class ArticleSearchPage(val items: List<AssistantArticle>, val hasMore: Boolean)

/** Short article details used for both search results and recommendations. */
@Serializable
data class AssistantArticle(
    val articleId: String,
    val title: String,
    val sourceName: String,
    /** Milliseconds since 1970-01-01 00:00 UTC; null if the date is unknown. */
    val dateMillis: Long? = null,
    val snippet: String,
)

/** An article ID picked by the model, with its reason. The app still needs to check it. */
@Serializable
data class ArticleRecommendation(val articleId: String, val reason: String)

/** The model's answer before the app checks the chosen IDs and search filters. */
@Serializable
data class AssistantAnswer(
    val kind: AssistantAnswerKind,
    val text: String,
    val articles: List<ArticleRecommendation> = emptyList(),
)

/** Fresh local article details paired with the model's recommendation reason. */
@Serializable
data class RecommendedArticle(val article: AssistantArticle, val reason: String)

/** A found article and the query that found it, kept for the final checks. */
data class SearchEvidence(
    val hit: AssistantArticle,
    val query: ArticleQuery,
)

/** The answer after local checks. Recommendation reasons are not fact-checked. */
data class ValidatedAssistantAnswer(
    val kind: AssistantAnswerKind,
    /** Used when asking for details or explaining an unsupported request; empty for search results. */
    val text: String,
    val articles: List<RecommendedArticle>,
    /** Some chosen articles were removed because they no longer matched or existed. */
    val resultsChanged: Boolean = false,
    /** Search found articles, even if none were chosen for the final answer. */
    val hadCandidates: Boolean = false,
)

/** Local search failed, no text fields can be searched, or all chosen articles failed rechecking. */
enum class ArticleSearchError :
    AssistantFailure { LOCAL_SEARCH, SEARCH_FIELDS_DISABLED, RESULTS_CHANGED }

/** Searching local articles, then choosing and ordering the results. */
enum class ArticleSearchPhase { SEARCHING, RANKING }

/** The current step and the number of articles found so far. */
data class ArticleSearchProgress(val phase: ArticleSearchPhase, val candidateCount: Int = 0) :
    AssistantProgress

/** Actions created by the app, such as opening a result in the reader. */
sealed interface ArticleSearchAction : AssistantAction {
    data class OpenArticle(val articleId: String) : ArticleSearchAction {
        override fun toString() = "OpenArticle([redacted])"
    }
}

/** Per-request limits for search calls, article counts and text length. */
object ArticleSearchLimits {
    const val SEARCH_RESULTS = 20
    const val CANDIDATES = 60
    const val RECOMMENDATIONS = 20
    const val SNIPPET_CHARS = 400
    const val REASON_CHARS = 120
}
