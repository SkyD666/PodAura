package com.skyd.podaura.ui.screen.feed.autodl

import com.skyd.podaura.ui.mvi.MviSingleEvent

sealed interface AutoDownloadRuleEvent : MviSingleEvent {
    sealed interface UpdateResultEvent : AutoDownloadRuleEvent {
        data class Failed(val msg: String) : UpdateResultEvent
    }
}