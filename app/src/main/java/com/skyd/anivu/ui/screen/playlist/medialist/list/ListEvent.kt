package com.skyd.anivu.ui.screen.playlist.medialist.list

import com.skyd.anivu.base.mvi.MviSingleEvent

sealed interface ListEvent : MviSingleEvent {
    sealed interface AddToPlaylistResultEvent : ListEvent {
        data object Success : AddToPlaylistResultEvent
        data class Failed(val msg: String) : AddToPlaylistResultEvent
    }
}