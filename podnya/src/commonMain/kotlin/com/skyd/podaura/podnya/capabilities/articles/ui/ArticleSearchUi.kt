package com.skyd.podaura.podnya.capabilities.articles.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchAction
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchContent
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchError
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchProgress
import com.skyd.podaura.podnya.capabilities.articles.model.repository.LocalArticleSearchRepository
import com.skyd.podaura.podnya.core.AssistantAction
import com.skyd.podaura.podnya.core.AssistantAnswerKind
import com.skyd.podaura.podnya.core.AssistantContent
import com.skyd.podaura.podnya.core.AssistantFailure
import com.skyd.podaura.podnya.core.AssistantProgress
import com.skyd.podaura.podnya.ui.AssistantCapabilityUi
import com.skyd.podaura.podnya.ui.PodnyaMarkdown
import com.skyd.podnya.resources.Res
import com.skyd.podnya.resources.podnya_missing_articles
import com.skyd.podnya.resources.podnya_no_matching_articles
import com.skyd.podnya.resources.podnya_no_suitable_recommendations
import com.skyd.podnya.resources.podnya_ok
import com.skyd.podnya.resources.podnya_recommendations
import com.skyd.podnya.resources.podnya_unranked
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

fun LazyListScope.articleSearchContent(
    turnId: String,
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
                if (fallbackText.isNotEmpty()) {
                    PodnyaMarkdown(text = fallbackText)
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
    recommendations.forEach { recommendation ->
        item(key = "$turnId-article-${recommendation.article.articleId}") {
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
        content.candidates.forEach { article ->
            item(key = "$turnId-candidate-${article.articleId}") {
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
    override fun itemKeys(
        turnId: String,
        content: AssistantContent,
        showFallbackText: Boolean
    ): List<String> = buildList {
        val value = content as ArticleSearchContent
        val answer = value.answer
        if ((showFallbackText && !answer?.text.isNullOrEmpty())
            || answer?.kind == AssistantAnswerKind.NO_MATCH
            || answer?.resultsChanged == true
        ) {
            add("$turnId-article-summary")
        }
        if (!answer?.articles.isNullOrEmpty()) add("$turnId-recommendations")
        answer?.articles?.forEach { add("$turnId-article-${it.article.articleId}") }
        if (value.candidates.isNotEmpty()) add("$turnId-fallback")
        value.candidates.forEach { add("$turnId-candidate-${it.articleId}") }
    }

    override fun LazyListScope.render(
        turnId: String, content: AssistantContent, showFallbackText: Boolean,
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

/** Opening an old card checks local existence only; viewing history never re-runs a search. */
@Composable
fun rememberArticleHistoryOpener(
    onOpen: (String) -> Unit,
    repository: LocalArticleSearchRepository = koinInject()
): (String) -> Unit {
    val scope = rememberCoroutineScope()
    val currentOpen by rememberUpdatedState(onOpen)
    var missing by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    if (missing || failed) {
        AlertDialog(
            onDismissRequest = { missing = false; failed = false },
            text = {
                Text(
                    if (missing) stringResource(Res.string.podnya_missing_articles)
                    else articleSearchErrorText(ArticleSearchError.LOCAL_SEARCH)
                )
            },
            confirmButton = {
                TextButton(onClick = { missing = false; failed = false }) {
                    Text(stringResource(Res.string.podnya_ok))
                }
            },
        )
    }
    return remember(repository, scope) {
        { id ->
            scope.launch {
                try {
                    if (repository.exists(id)) currentOpen(id) else missing = true
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    failed = true
                }
            }
        }
    }
}
