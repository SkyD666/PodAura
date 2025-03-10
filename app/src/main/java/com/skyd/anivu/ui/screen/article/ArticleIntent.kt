package com.skyd.anivu.ui.screen.article

import com.skyd.anivu.base.mvi.MviIntent
import com.skyd.anivu.model.repository.ArticleSort

sealed interface ArticleIntent : MviIntent {
    data class Init(
        val urls: List<String>,
        val groupIds: List<String>,
        val articleIds: List<String>,
    ) : ArticleIntent

    data class UpdateSort(val articleSort: ArticleSort) : ArticleIntent
    data class Refresh(val urls: List<String>) : ArticleIntent
    data class Favorite(val articleId: String, val favorite: Boolean) : ArticleIntent
    data class Read(val articleId: String, val read: Boolean) : ArticleIntent
    data class FilterFavorite(val favorite: Boolean?) : ArticleIntent
    data class FilterRead(val read: Boolean?) : ArticleIntent
}