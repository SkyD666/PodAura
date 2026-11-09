package com.skyd.podaura.podnya.capabilities.articles

import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchContent
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchLimits
import com.skyd.podaura.podnya.capabilities.articles.model.bean.RecommendedArticle
import com.skyd.podaura.podnya.core.AssistantHistoryTurn
import com.skyd.podaura.podnya.core.AssistantLimits
import com.skyd.podaura.podnya.model.bean.forCapability
import kotlinx.serialization.Serializable

/** Read only article history and keep its text and article count within the limits. */
internal fun articleSearchHistory(history: List<AssistantHistoryTurn>): List<ArticleHistoryTurn> {
    var remaining = ArticleSearchLimits.CANDIDATES
    return history.takeLast(AssistantLimits.HISTORY_TURNS).asReversed().map { turn ->
        val articles = ArticleSearchContent.historyArticles(turn.content.forCapability(ArticleSearchContent.CAPABILITY_ID))
            .take(remaining.coerceAtMost(ArticleSearchLimits.RECOMMENDATIONS))
            .filter { it.article.articleId.isNotBlank() && it.article.articleId.length <= 256 }
            .map {
                it.copy(
                    article = it.article.copy(
                        title = it.article.title.take(200),
                        sourceName = it.article.sourceName.take(100),
                        snippet = it.article.snippet.take(ArticleSearchLimits.SNIPPET_CHARS),
                    ), reason = it.reason.take(ArticleSearchLimits.REASON_CHARS)
                )
            }
        remaining -= articles.size
        ArticleHistoryTurn(
            question = turn.question.take(AssistantLimits.INPUT_CHARS),
            reply = turn.reply.take(AssistantLimits.REPLY_CHARS), articles = articles
        )
    }.asReversed()
}

@Serializable
internal data class ArticleHistoryTurn(
    val question: String, val reply: String, val articles: List<RecommendedArticle>,
)
