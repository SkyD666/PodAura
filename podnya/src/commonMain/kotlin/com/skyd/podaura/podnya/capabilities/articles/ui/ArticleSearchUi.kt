package com.skyd.podaura.podnya.capabilities.articles.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchAction
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchContent
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchError
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchProgress
import com.skyd.podaura.podnya.core.AssistantAction
import com.skyd.podaura.podnya.core.AssistantAnswerKind
import com.skyd.podaura.podnya.core.AssistantContent
import com.skyd.podaura.podnya.core.AssistantFailure
import com.skyd.podaura.podnya.core.AssistantProgress
import com.skyd.podaura.podnya.ui.AssistantCapabilityUi
import com.skyd.podnya.resources.Res
import com.skyd.podnya.resources.podnya_no_matching_articles
import com.skyd.podnya.resources.podnya_no_suitable_recommendations
import com.skyd.podnya.resources.podnya_recommendations
import com.skyd.podnya.resources.podnya_unranked
import org.jetbrains.compose.resources.stringResource

fun LazyListScope.articleSearchContent(
    turnId: Long,
    content: ArticleSearchContent,
    showFallbackText: Boolean,
    formatDate: (Long) -> String,
    onAction: (ArticleSearchAction) -> Unit,
) {
    val answer = content.answer
    val fallbackText = if (showFallbackText) answer?.text.orEmpty() else ""
    if (fallbackText.isNotEmpty() || answer?.kind == AssistantAnswerKind.NO_MATCH || answer?.resultsChanged == true) {
        item(key = "$turnId-article-summary") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (fallbackText.isNotEmpty()) SelectionContainer {
                    Text(fallbackText, style = MaterialTheme.typography.bodyLarge)
                }
                if (answer?.kind == AssistantAnswerKind.NO_MATCH) {
                    Text(
                        stringResource(
                            if (answer.hadCandidates) Res.string.podnya_no_suitable_recommendations
                            else Res.string.podnya_no_matching_articles
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (answer?.resultsChanged == true) {
                    Text(
                        articleSearchErrorText(ArticleSearchError.RESULTS_CHANGED),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    val recommendations = answer?.articles.orEmpty()
    if (recommendations.isNotEmpty()) item(key = "$turnId-recommendations") {
        Text(
            stringResource(Res.string.podnya_recommendations, recommendations.size),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
    recommendations.forEachIndexed { index, recommendation ->
        item(key = "$turnId-article-$index") {
            ArticleRecommendationItem(recommendation.article, recommendation.reason, formatDate) {
                onAction(ArticleSearchAction.OpenArticle(recommendation.article.articleId))
            }
        }
    }
    if (content.candidates.isNotEmpty()) {
        item(key = "$turnId-fallback") {
            Text(
                stringResource(Res.string.podnya_unranked),
                style = MaterialTheme.typography.labelLarge
            )
        }
        content.candidates.forEachIndexed { index, article ->
            item(key = "$turnId-candidate-$index") {
                ArticleRecommendationItem(
                    article,
                    null,
                    formatDate
                ) { onAction(ArticleSearchAction.OpenArticle(article.articleId)) }
            }
        }
    }
}

internal object ArticleSearchUi : AssistantCapabilityUi {
    override fun supports(content: AssistantContent) = content is ArticleSearchContent
    override fun LazyListScope.render(
        turnId: Long, content: AssistantContent, showFallbackText: Boolean,
        formatDate: (Long) -> String, onAction: (AssistantAction) -> Unit,
    ) = articleSearchContent(
        turnId,
        content as ArticleSearchContent,
        showFallbackText,
        formatDate,
        onAction
    )

    @Composable
    override fun progressText(progress: AssistantProgress?): String? =
        (progress as? ArticleSearchProgress)?.let { articleSearchProgressText(it) }

    @Composable
    override fun failureText(error: AssistantFailure): String? =
        (error as? ArticleSearchError)?.let { articleSearchErrorText(it) }
}
