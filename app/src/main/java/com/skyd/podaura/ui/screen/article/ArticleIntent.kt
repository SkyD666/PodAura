package com.skyd.podaura.ui.screen.article

import com.skyd.mvi.MviIntent

sealed interface ArticleIntent : MviIntent {
    data class Init(
        val feedUrls: List<String>,
        val groupIds: List<String>,
        val articleIds: List<String>,
    ) : ArticleIntent

    data class UpdateFilter(
        val feedUrls: List<String>,
        val groupIds: List<String>,
        val articleIds: List<String>,
        val filterMask: Int,
    ) : ArticleIntent

    data class Refresh(
        val feedUrls: List<String>,
        val groupIds: List<String>,
        val articleIds: List<String>,
    ) : ArticleIntent

    data class Favorite(val articleId: String, val favorite: Boolean) : ArticleIntent
    data class Read(val articleId: String, val read: Boolean) : ArticleIntent
    data class Delete(val articleId: String) : ArticleIntent
}