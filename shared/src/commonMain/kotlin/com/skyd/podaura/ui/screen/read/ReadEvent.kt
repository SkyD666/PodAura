package com.skyd.podaura.ui.screen.read

import com.skyd.mvi.MviSingleEvent

sealed interface ReadEvent : MviSingleEvent {
    sealed interface FavoriteArticleResultEvent : ReadEvent {
        data class Failed(val msg: String) : FavoriteArticleResultEvent
    }

    sealed interface ReadArticleResultEvent : ReadEvent {
        data class Failed(val msg: String) : ReadArticleResultEvent
    }

}
