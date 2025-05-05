package com.skyd.podaura.ui.screen.history.search

import com.skyd.podaura.ui.mvi.MviSingleEvent

sealed interface HistorySearchEvent : MviSingleEvent {
    sealed interface DeleteReadHistoryResultEvent : HistorySearchEvent {
        data class Failed(val msg: String) : DeleteReadHistoryResultEvent
    }

    sealed interface DeleteMediaPlayHistoryResultEvent : HistorySearchEvent {
        data class Failed(val msg: String) : DeleteMediaPlayHistoryResultEvent
    }
}