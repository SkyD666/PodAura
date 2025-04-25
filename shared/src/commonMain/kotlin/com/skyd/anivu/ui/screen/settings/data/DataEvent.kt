package com.skyd.anivu.ui.screen.settings.data

import com.skyd.anivu.ui.mvi.MviSingleEvent

sealed interface DataEvent : MviSingleEvent {
    sealed interface ClearCacheResultEvent : DataEvent {
        data class Success(val msg: String) : ClearCacheResultEvent
        data class Failed(val msg: String) : ClearCacheResultEvent
    }

    sealed interface DeletePlayHistoryResultEvent : DataEvent {
        data class Success(val count: Int) : DeletePlayHistoryResultEvent
        data class Failed(val msg: String) : DeletePlayHistoryResultEvent
    }

    sealed interface DeleteArticleBeforeResultEvent : DataEvent {
        data class Success(val msg: String) : DeleteArticleBeforeResultEvent
        data class Failed(val msg: String) : DeleteArticleBeforeResultEvent
    }
}