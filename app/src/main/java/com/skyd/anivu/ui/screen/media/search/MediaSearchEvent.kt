package com.skyd.anivu.ui.screen.media.search

import com.skyd.anivu.ui.mvi.MviSingleEvent

sealed interface MediaSearchEvent : MviSingleEvent {
    sealed interface DeleteFileResultEvent : MediaSearchEvent {
        data class Failed(val msg: String) : DeleteFileResultEvent
    }

    sealed interface RenameFileResultEvent : MediaSearchEvent {
        data class Failed(val msg: String) : DeleteFileResultEvent
    }
}