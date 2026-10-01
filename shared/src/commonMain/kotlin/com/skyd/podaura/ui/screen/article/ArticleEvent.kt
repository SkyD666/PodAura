package com.skyd.podaura.ui.screen.article

import com.skyd.mvi.MviSingleEvent
import com.skyd.podaura.model.repository.BatchProgress
import com.skyd.podaura.model.repository.article.ArticleRepository.SelectedPlaylistMedia
import com.skyd.podaura.model.repository.download.SelectedDownloadResult

sealed interface ArticleEvent : MviSingleEvent {
    sealed interface SelectionResultEvent : ArticleEvent {
        data class Downloaded(val result: SelectedDownloadResult) : SelectionResultEvent
        data class Completed(val result: BatchProgress) : SelectionResultEvent
        data class Cancelled(val progress: BatchProgress?) : SelectionResultEvent
        data class PlaylistPrepared(val result: SelectedPlaylistMedia) : SelectionResultEvent
        data class Failed(val msg: String) : SelectionResultEvent
    }
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
        data object ProtectedByDownload : DeleteArticleResultEvent
        data class Failed(val msg: String) : DeleteArticleResultEvent
    }
}
