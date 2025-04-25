package com.skyd.anivu.ui.screen.feed.requestheaders

import com.skyd.anivu.ui.mvi.MviSingleEvent

sealed interface RequestHeadersEvent : MviSingleEvent {
    sealed interface HeadersResultEvent : RequestHeadersEvent {
        data class Failed(val msg: String) : HeadersResultEvent
    }

    sealed interface UpdateHeadersResultEvent : RequestHeadersEvent {
        data class Failed(val msg: String) : UpdateHeadersResultEvent
    }
}