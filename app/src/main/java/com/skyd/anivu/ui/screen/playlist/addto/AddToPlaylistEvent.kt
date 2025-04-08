package com.skyd.anivu.ui.screen.playlist.addto

import com.skyd.anivu.base.mvi.MviSingleEvent

sealed interface AddToPlaylistEvent : MviSingleEvent {
    sealed interface AddToPlaylistResultEvent : AddToPlaylistEvent {
        data object Success : AddToPlaylistResultEvent
        data class Failed(val msg: String) : AddToPlaylistResultEvent
    }
}