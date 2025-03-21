package com.skyd.anivu.ui.screen.read

import com.skyd.anivu.base.mvi.MviSingleEvent

sealed interface ReadEvent : MviSingleEvent {
    sealed interface FavoriteArticleResultEvent : ReadEvent {
        data class Failed(val msg: String) : FavoriteArticleResultEvent
    }

    sealed interface ReadArticleResultEvent : ReadEvent {
        data class Failed(val msg: String) : ReadArticleResultEvent
    }

    sealed interface ShareImageResultEvent : ReadEvent {
        data class Failed(val msg: String) : ShareImageResultEvent
    }

    sealed interface CopyImageResultEvent : ReadEvent {
        data class Success(val url: String) : CopyImageResultEvent
        data class Failed(val msg: String) : CopyImageResultEvent
    }

    sealed interface DownloadImageResultEvent : ReadEvent {
        data class Success(val url: String) : ReadArticleResultEvent
        data class Failed(val msg: String) : ReadArticleResultEvent
    }
}