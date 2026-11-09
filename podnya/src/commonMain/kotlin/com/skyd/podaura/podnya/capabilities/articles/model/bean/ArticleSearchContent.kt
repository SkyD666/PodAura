package com.skyd.podaura.podnya.capabilities.articles.model.bean

import com.skyd.podaura.podnya.core.AssistantContent
import com.skyd.podaura.podnya.core.AssistantHistorySnapshot
import com.skyd.podaura.podnya.core.AssistantLimits
import kotlinx.serialization.json.Json

/** Only this capability knows how to bound, remember and render article results. */
class ArticleSearchContent private constructor(
    val answer: ValidatedAssistantAnswer?,
    val candidates: List<AssistantArticle>,
) : AssistantContent {
    override val retainedBytes: Int = (answer?.text?.bytes() ?: 0) +
            (answer?.articles?.sumOf { it.article.bytes() + it.reason.bytes() } ?: 0) +
            candidates.sumOf { it.bytes() } + 128

    override fun historySnapshot(): AssistantHistorySnapshot? = answer?.let {
        AssistantHistorySnapshot(
            CAPABILITY_ID,
            it.text.ifBlank { it.kind.name },
            Json.encodeToString(it.articles)
        )
    }

    companion object {
        const val CAPABILITY_ID = "articles"

        fun recommendations(answer: ValidatedAssistantAnswer): ArticleSearchContent {
            var bounded = answer.copy(
                text = answer.text.take(AssistantLimits.REPLY_CHARS),
                articles = answer.articles.take(ArticleSearchLimits.RECOMMENDATIONS).map {
                    it.copy(
                        article = it.article.bounded(),
                        reason = it.reason.take(ArticleSearchLimits.REASON_CHARS)
                    )
                },
            )
            while (ArticleSearchContent(bounded, emptyList())
                    .retainedBytes > AssistantLimits.CONTENT_BYTES &&
                bounded.articles.isNotEmpty()
            ) {
                bounded = bounded.copy(
                    articles = bounded.articles.dropLast(1),
                    resultsChanged = true
                )
            }
            return ArticleSearchContent(bounded, emptyList())
        }

        fun unranked(candidates: List<AssistantArticle>): ArticleSearchContent {
            var bounded = candidates.take(ArticleSearchLimits.RECOMMENDATIONS).map { it.bounded() }
            while (ArticleSearchContent(null, bounded)
                    .retainedBytes > AssistantLimits.CONTENT_BYTES && bounded.isNotEmpty()
            ) {
                bounded = bounded.dropLast(1)
            }
            return ArticleSearchContent(null, bounded)
        }

        fun historyArticles(snapshot: AssistantHistorySnapshot?): List<RecommendedArticle> =
            if (snapshot?.capabilityId != CAPABILITY_ID) emptyList()
            else runCatching { Json.decodeFromString<List<RecommendedArticle>>(snapshot.payload) }
                .getOrDefault(emptyList())

        private fun AssistantArticle.bounded() =
            copy(snippet = snippet.take(ArticleSearchLimits.SNIPPET_CHARS))

        private fun String.bytes() = encodeToByteArray().size
        private fun AssistantArticle.bytes() =
            articleId.bytes() + title.bytes() + sourceName.bytes() + snippet.bytes() + 32
    }
}
