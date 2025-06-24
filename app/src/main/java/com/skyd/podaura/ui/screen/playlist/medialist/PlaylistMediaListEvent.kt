package com.skyd.podaura.ui.screen.playlist.medialist

import com.skyd.mvi.MviSingleEvent

sealed interface PlaylistMediaListEvent : MviSingleEvent {
    sealed interface ReorderResultEvent : PlaylistMediaListEvent {
        data class Failed(val msg: String) : ReorderResultEvent
    }

    sealed interface DeleteResultEvent : PlaylistMediaListEvent {
        data class Failed(val msg: String) : DeleteResultEvent
    }
}