package com.skyd.podaura.podnya.capabilities.articles.ui

import androidx.compose.runtime.Composable
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchError
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchPhase
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchProgress
import com.skyd.podnya.resources.Res
import com.skyd.podnya.resources.podnya_error_local_search
import com.skyd.podnya.resources.podnya_error_results_changed
import com.skyd.podnya.resources.podnya_error_search_fields_disabled
import com.skyd.podnya.resources.podnya_ranking
import com.skyd.podnya.resources.podnya_searching
import org.jetbrains.compose.resources.stringResource

@Composable
fun articleSearchProgressText(progress: ArticleSearchProgress): String = stringResource(
    when (progress.phase) {
        ArticleSearchPhase.SEARCHING -> Res.string.podnya_searching
        ArticleSearchPhase.RANKING -> Res.string.podnya_ranking
    },
    progress.candidateCount.coerceAtLeast(0),
)

@Composable
fun articleSearchErrorText(error: ArticleSearchError): String = stringResource(
    when (error) {
        ArticleSearchError.LOCAL_SEARCH -> Res.string.podnya_error_local_search
        ArticleSearchError.SEARCH_FIELDS_DISABLED -> Res.string.podnya_error_search_fields_disabled
        ArticleSearchError.RESULTS_CHANGED -> Res.string.podnya_error_results_changed
    },
)
