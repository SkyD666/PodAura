package com.skyd.podaura.ui.screen.search

import com.skyd.podaura.ui.mvi.MviSingleEvent

sealed interface SearchEvent : MviSingleEvent {
    sealed interface FavoriteArticleResultEvent : SearchEvent {
        data class Failed(val msg: String) : FavoriteArticleResultEvent
    }

    sealed interface ReadArticleResultEvent : SearchEvent {
        data class Failed(val msg: String) : ReadArticleResultEvent
    }

    sealed interface DeleteArticleResultEvent : SearchEvent {
        data class Failed(val msg: String) : DeleteArticleResultEvent
    }
}