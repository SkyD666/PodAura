package com.skyd.anivu.model.repository.article

import kotlinx.coroutines.flow.Flow

interface IArticleRepository {
    suspend fun getFeedUrls(feedUrls: List<String>, groupIds: List<String>): List<String>

    fun refreshArticleList(feedUrls: List<String>, full: Boolean): Flow<Unit>

    fun refreshGroupArticles(groupId: String?, full: Boolean): Flow<Unit>

    fun readArticle(articleId: String, read: Boolean): Flow<Unit>

    fun favoriteArticle(articleId: String, favorite: Boolean): Flow<Unit>

    fun deleteArticle(articleId: String): Flow<Int>
}