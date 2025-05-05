package com.skyd.podaura.ui.screen.article

import com.skyd.podaura.ui.mvi.MviSingleEvent

sealed interface ArticleEvent : MviSingleEvent {
    sealed interface InitArticleListResultEvent : ArticleEvent {
        data class Failed(val msg: String) : InitArticleListResultEvent
    }

    sealed interface RefreshArticleListResultEvent : ArticleEvent {
        data class Failed(val msg: String) : RefreshArticleListResultEvent
    }

    sealed interface FavoriteArticleResultEvent : ArticleEvent {
        data class Failed(val msg: String) : FavoriteArticleResultEvent
    }

    sealed interface ReadArticleResultEvent : ArticleEvent {
        data class Failed(val msg: String) : ReadArticleResultEvent
    }

    sealed interface DeleteArticleResultEvent : ArticleEvent {
        data class Failed(val msg: String) : DeleteArticleResultEvent
    }
}