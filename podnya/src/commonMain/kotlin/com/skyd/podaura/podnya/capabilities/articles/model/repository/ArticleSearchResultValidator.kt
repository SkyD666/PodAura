package com.skyd.podaura.podnya.capabilities.articles.model.repository

import com.skyd.podaura.api.model.bean.article.ArticleSearchScope
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchError
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchLimits
import com.skyd.podaura.podnya.capabilities.articles.model.bean.AssistantAnswer
import com.skyd.podaura.podnya.capabilities.articles.model.bean.AssistantArticle
import com.skyd.podaura.podnya.capabilities.articles.model.bean.RecommendedArticle
import com.skyd.podaura.podnya.capabilities.articles.model.bean.SearchEvidence
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ValidatedAssistantAnswer
import com.skyd.podaura.podnya.core.AssistantAnswerKind
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantException
import com.skyd.podaura.podnya.core.AssistantLimits

/** Check the model's article choices and build results from current local data. */
class ArticleSearchResultValidator(private val source: LocalArticleSearchRepository) {
    suspend fun validate(
        answer: AssistantAnswer,
        scope: ArticleSearchScope,
        evidence: Map<String, SearchEvidence>,
        successfulSearches: Int,
    ): ValidatedAssistantAnswer {
        val needsExplanation = answer.kind == AssistantAnswerKind.CLARIFICATION ||
                answer.kind == AssistantAnswerKind.UNSUPPORTED

        // Check required text, text length and the number of recommendations.
        val hasInvalidContent = (needsExplanation && answer.text.isBlank()) ||
                answer.text.length > AssistantLimits.REPLY_CHARS ||
                answer.articles.size > ArticleSearchLimits.RECOMMENDATIONS ||
                answer.articles.any { it.reason.isBlank() || it.reason.length > ArticleSearchLimits.REASON_CHARS }
        if (hasInvalidContent) invalid()

        // The answer type must agree with its articles and completed searches.
        if (answer.kind != AssistantAnswerKind.RESULTS && answer.articles.isNotEmpty()) invalid()
        if (answer.kind == AssistantAnswerKind.NO_MATCH && successfulSearches == 0) invalid()
        if (answer.kind == AssistantAnswerKind.RESULTS && (successfulSearches == 0 || answer.articles.isEmpty())) invalid()

        // Only accept IDs returned by this turn's searches. Reject the answer if any ID is unknown.
        if (answer.articles.any { it.articleId !in evidence }) invalid()

        // Remove repeated IDs while keeping the model's order.
        val selected = answer.articles.distinctBy { it.articleId }
        val refreshed = mutableMapOf<String, AssistantArticle>()

        // Recheck each group using the query that found it and the app's search scope.
        selected.groupBy { evidence.getValue(it.articleId).query }.forEach { (query, choices) ->
            val requested = choices.map { it.articleId }.toSet()
            source.resolve(scope, requested.toList(), query)
                .filter { it.articleId in requested }
                .forEach { refreshed[it.articleId] = it }
        }

        // Use fresh article details and drop articles that no longer exist or match.
        // Keep the model's reasons; these checks do not verify the facts in those reasons.
        val articles = selected.mapNotNull { recommendation ->
            refreshed[recommendation.articleId]?.let {
                RecommendedArticle(it, recommendation.reason)
            }
        }

        // All chosen articles became unavailable; this is not a normal no-match result.
        if (answer.kind == AssistantAnswerKind.RESULTS && articles.isEmpty()) {
            throw AssistantException(ArticleSearchError.RESULTS_CHANGED)
        }

        return ValidatedAssistantAnswer(
            kind = answer.kind,
            // Keep text only for requests that need more details or are not supported.
            // Search results get a new streamed reply based on the checked articles.
            text = if (needsExplanation) answer.text else "",
            articles = articles,
            resultsChanged = articles.size != selected.size,
            hadCandidates = evidence.isNotEmpty(),
        )
    }

    private fun invalid(): Nothing = throw AssistantException(AssistantError.INVALID_OUTPUT)
}
