package com.skyd.podaura.ui.screen.history.search

import com.skyd.podaura.ui.mvi.MviIntent

sealed interface HistorySearchIntent : MviIntent {
    data class Query(val query: String) : HistorySearchIntent
    data class DeleteReadHistory(val articleId: String) : HistorySearchIntent
    data class DeleteMediaPlayHistory(val path: String) : HistorySearchIntent
}