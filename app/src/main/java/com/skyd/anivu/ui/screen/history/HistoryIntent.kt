package com.skyd.anivu.ui.screen.history

import com.skyd.anivu.base.mvi.MviIntent

sealed interface HistoryIntent : MviIntent {
    data object Init : HistoryIntent
    data class DeleteReadHistory(val articleId: String) : HistoryIntent
    data class DeleteMediaPlayHistory(val path: String) : HistoryIntent
    data object DeleteAllReadHistory : HistoryIntent
    data object DeleteAllMediaPlayHistory : HistoryIntent
}