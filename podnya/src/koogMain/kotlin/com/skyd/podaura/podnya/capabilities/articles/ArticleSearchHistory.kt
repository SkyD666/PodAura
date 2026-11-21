package com.skyd.podaura.podnya.capabilities.articles

import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleReferences
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchContent
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchLimits
import com.skyd.podaura.podnya.capabilities.articles.model.bean.AssistantArticle
import com.skyd.podaura.podnya.capabilities.articles.model.bean.RecommendedArticle
import com.skyd.podaura.podnya.core.AssistantHistoryTurn
import com.skyd.podaura.podnya.core.AssistantLimits
import com.skyd.podaura.podnya.model.bean.forCapability
import kotlinx.serialization.Serializable

/** Read only article history and keep its text and article count within the limits. */
internal fun articleSearchHistory(history: List<AssistantHistoryTurn>): List<ArticleHistoryTurn> {
    val seenIds = mutableSetOf<String>()
    return history.takeLast(AssistantLimits.HISTORY_TURNS).asReversed().map { turn ->
        val snapshot = turn.content.forCapability(ArticleSearchContent.CAPABILITY_ID)
        val remembered = if (snapshot?.payload?.startsWith("{") == true) {
            ArticleReferences.from(snapshot)?.items.orEmpty().map {
                RecommendedArticle(
                    article = AssistantArticle(
                        articleId = it.id,
                        title = it.title,
                        sourceName = "",
                        snippet = "",
                    ),
                    reason = "",
                )
            }
        } else {
            ArticleSearchContent.historyArticles(snapshot)
        }
        val articles = remembered
            .take(ArticleSearchLimits.RECOMMENDATIONS)
            .filter { it.article.articleId.isNotBlank() && it.article.articleId.length <= 256 }
            .filter { seenIds.size < ArticleSearchLimits.CANDIDATES && seenIds.add(it.article.articleId) }
            .map {
                it.copy(
                    article = it.article.copy(
                        title = it.article.title.take(200),
                        sourceName = it.article.sourceName.take(100),
                        snippet = it.article.snippet.take(ArticleSearchLimits.SNIPPET_CHARS),
                    ), reason = it.reason.take(ArticleSearchLimits.REASON_CHARS)
                )
            }
        ArticleHistoryTurn(
            question = turn.question.take(AssistantLimits.INPUT_CHARS),
            reply = turn.reply.take(AssistantLimits.HISTORY_REPLY_CHARS), articles = articles
        )
    }.asReversed()
}

@Serializable
internal data class ArticleHistoryTurn(
    val question: String, val reply: String, val articles: List<RecommendedArticle>,
)
