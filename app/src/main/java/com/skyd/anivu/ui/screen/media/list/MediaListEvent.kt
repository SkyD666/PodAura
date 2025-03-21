package com.skyd.anivu.ui.screen.media.list

import com.skyd.anivu.base.mvi.MviSingleEvent

sealed interface MediaListEvent : MviSingleEvent {
    sealed interface MediaListResultEvent : MediaListEvent {
        data class Failed(val msg: String) : MediaListResultEvent
    }

    sealed interface DeleteFileResultEvent : MediaListEvent {
        data class Failed(val msg: String) : DeleteFileResultEvent
    }
}