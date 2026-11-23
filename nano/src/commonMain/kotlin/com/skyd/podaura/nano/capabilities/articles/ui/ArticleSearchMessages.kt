package com.skyd.podaura.nano.capabilities.articles.ui

import androidx.compose.runtime.Composable
import com.skyd.nano.resources.Res
import com.skyd.nano.resources.nano_error_local_search
import com.skyd.nano.resources.nano_error_results_changed
import com.skyd.nano.resources.nano_error_search_fields_disabled
import com.skyd.nano.resources.nano_ranking
import com.skyd.nano.resources.nano_searching
import com.skyd.podaura.nano.capabilities.articles.model.bean.ArticleSearchError
import com.skyd.podaura.nano.capabilities.articles.model.bean.ArticleSearchPhase
import com.skyd.podaura.nano.capabilities.articles.model.bean.ArticleSearchProgress
import org.jetbrains.compose.resources.stringResource

@Composable
fun articleSearchProgressText(progress: ArticleSearchProgress): String = stringResource(
    when (progress.phase) {
        ArticleSearchPhase.SEARCHING -> Res.string.nano_searching
        ArticleSearchPhase.RANKING -> Res.string.nano_ranking
    },
    progress.candidateCount.coerceAtLeast(0),
)

@Composable
fun articleSearchErrorText(error: ArticleSearchError): String = stringResource(
    when (error) {
        ArticleSearchError.LOCAL_SEARCH -> Res.string.nano_error_local_search
        ArticleSearchError.SEARCH_FIELDS_DISABLED -> Res.string.nano_error_search_fields_disabled
        ArticleSearchError.RESULTS_CHANGED -> Res.string.nano_error_results_changed
    },
)
