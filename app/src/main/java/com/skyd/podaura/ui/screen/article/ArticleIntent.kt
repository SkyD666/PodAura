package com.skyd.podaura.ui.screen.article

import com.skyd.podaura.model.repository.article.ArticleSort
import com.skyd.podaura.ui.mvi.MviIntent

sealed interface ArticleIntent : MviIntent {
    data class Init(
        val urls: List<String>,
        val groupIds: List<String>,
        val articleIds: List<String>,
    ) : ArticleIntent

    data class UpdateSort(val articleSort: ArticleSort) : ArticleIntent
    data class Refresh(
        val feedUrls: List<String>,
        val groupIds: List<String>,
        val articleIds: List<String>,
    ) : ArticleIntent

    data class Favorite(val articleId: String, val favorite: Boolean) : ArticleIntent
    data class Read(val articleId: String, val read: Boolean) : ArticleIntent
    data class Delete(val articleId: String) : ArticleIntent
    data class FilterFavorite(val favorite: Boolean?) : ArticleIntent
    data class FilterRead(val read: Boolean?) : ArticleIntent
}