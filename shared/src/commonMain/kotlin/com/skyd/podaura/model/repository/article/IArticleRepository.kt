package com.skyd.podaura.model.repository.article

import com.skyd.podaura.model.bean.article.ArticleDeleteResult
import kotlinx.coroutines.flow.Flow

interface IArticleRepository {
    fun requestRealFeedUrls(
        feedUrls: List<String>,
        groupIds: List<String>,
        articleIds: List<String>,
    ): Flow<List<String>>

    fun refreshArticleList(feedUrls: List<String>, full: Boolean): Flow<Unit>

    fun refreshGroupArticles(groupId: String?, full: Boolean): Flow<Unit>

    fun readArticle(articleId: String, read: Boolean): Flow<Unit>

    fun observeArticleFavorite(articleId: String): Flow<Boolean?>

    fun favoriteArticle(articleId: String, favorite: Boolean): Flow<Unit>

    fun deleteArticle(articleId: String): Flow<ArticleDeleteResult>
}
