package com.skyd.podaura.ui.screen.search

import com.skyd.podaura.ui.mvi.MviIntent

sealed interface SearchIntent : MviIntent {
    data class UpdateQuery(val query: String) : SearchIntent
    data class UpdateSort(val dateDesc: Boolean) : SearchIntent
    data object ListenSearchFeed : SearchIntent
    data class ListenSearchArticle(
        val feedUrls: List<String>,
        val groupIds: List<String>,
        val articleIds: List<String>,
    ) : SearchIntent

    data class Favorite(val articleId: String, val favorite: Boolean) : SearchIntent
    data class Read(val articleId: String, val read: Boolean) : SearchIntent
    data class Delete(val articleId: String) : SearchIntent
}