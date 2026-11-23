package com.skyd.podaura.model.repository.article

import com.skyd.podaura.api.model.bean.article.ArticleQuery
import com.skyd.podaura.api.model.bean.article.ArticleRecord
import com.skyd.podaura.api.model.bean.article.ArticleSearchScope
import com.skyd.podaura.api.model.repository.IArticleRepository
import com.skyd.podaura.config.allSearchDomain
import com.skyd.podaura.model.bean.article.ARTICLE_TABLE_NAME
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.db.dao.SearchDomainDao
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Adapts the host's schema and enabled search columns to the public article query interface. */
class ArticleQueryRepository(
    private val articleDao: ArticleDao,
    private val searchDomainDao: SearchDomainDao,
) : IArticleRepository {
    override suspend fun query(
        scope: ArticleSearchScope,
        query: ArticleQuery
    ): List<ArticleRecord> {
        require(query.limit > 0) { "Article limit must be positive" }
        val columns = if (query.keywords.isEmpty()) emptyList() else enabledColumns()
        var best = emptyList<ArticleWithFeed>()
        for (part in scope.chunks()) {
            currentCoroutineContext().ensureActive()
            best = (best + articleDao.getArticleList(
                buildArticleRepositoryQuery(
                    scope = part,
                    query = query,
                    searchColumns = columns
                )
            )).distinctBy { it.articleWithEnclosure.article.articleId }
                .sortedWith(compareByDescending<ArticleWithFeed> { it.articleWithEnclosure.article.date }
                    .thenBy { it.articleWithEnclosure.article.articleId })
                .take(query.limit)
        }
        return best.map { row ->
            currentCoroutineContext().ensureActive()
            val article = row.articleWithEnclosure.article
            ArticleRecord(
                articleId = article.articleId,
                title = article.title,
                sourceTitle = row.feed.title,
                sourceNickname = row.feed.nickname,
                dateMillis = article.date,
                content = article.content,
                description = article.description,
            )
        }
    }

    private suspend fun enabledColumns(): List<String> {
        val domains = searchDomainDao.getAllSearchDomain()
        val whitelist = allSearchDomain.getValue(ARTICLE_TABLE_NAME)
        return if (domains.isEmpty()) whitelist else domains
            .filter { it.tableName == ARTICLE_TABLE_NAME && it.search && it.columnName in whitelist }
            .map { it.columnName }
    }
}
