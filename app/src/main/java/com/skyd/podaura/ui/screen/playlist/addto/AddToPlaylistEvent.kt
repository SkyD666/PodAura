package com.skyd.podaura.ui.screen.playlist.addto

import com.skyd.podaura.ui.mvi.MviSingleEvent

sealed interface AddToPlaylistEvent : MviSingleEvent {
    sealed interface AddToPlaylistResultEvent : AddToPlaylistEvent {
        data object Success : AddToPlaylistResultEvent
        data class Failed(val msg: String) : AddToPlaylistResultEvent
    }
}