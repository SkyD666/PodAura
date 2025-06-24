package com.skyd.podaura.ui.screen.history

import com.skyd.mvi.MviIntent

sealed interface HistoryIntent : MviIntent {
    data object Init : HistoryIntent
    data class DeleteReadHistory(val articleId: String) : HistoryIntent
    data class DeleteMediaPlayHistory(val path: String) : HistoryIntent
    data object DeleteAllReadHistory : HistoryIntent
    data object DeleteAllMediaPlayHistory : HistoryIntent
}