package com.skyd.podaura.ui.screen.history

import com.skyd.podaura.ui.mvi.MviSingleEvent

sealed interface HistoryEvent : MviSingleEvent {
    sealed interface DeleteReadHistory : HistoryEvent {
        data class Failed(val msg: String) : DeleteReadHistory
    }

    sealed interface DeleteMediaPlayHistory : HistoryEvent {
        data class Failed(val msg: String) : DeleteMediaPlayHistory
    }
}