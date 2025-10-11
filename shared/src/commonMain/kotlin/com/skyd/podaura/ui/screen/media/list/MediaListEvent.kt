package com.skyd.podaura.ui.screen.media.list

import com.skyd.mvi.MviSingleEvent

sealed interface MediaListEvent : MviSingleEvent {
    sealed interface MediaListResultEvent : MediaListEvent {
        data class Failed(val msg: String) : MediaListResultEvent
    }

    sealed interface DeleteFileResultEvent : MediaListEvent {
        data class Failed(val msg: String) : DeleteFileResultEvent
    }
}