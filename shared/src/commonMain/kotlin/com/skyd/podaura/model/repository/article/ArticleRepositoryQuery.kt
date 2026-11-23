package com.skyd.podaura.model.repository.article

import androidx.room3.RoomRawQuery
import com.skyd.podaura.api.model.bean.KeywordMatch
import com.skyd.podaura.api.model.bean.article.ArticleQuery
import com.skyd.podaura.api.model.bean.article.ArticleSearchScope
import com.skyd.podaura.api.model.repository.ArticleSearchFieldsDisabledException
import com.skyd.podaura.config.allSearchDomain
import com.skyd.podaura.model.bean.article.ARTICLE_TABLE_NAME
import com.skyd.podaura.model.bean.article.ArticleBean

/** Fixed identifiers and bound values; deliberately independent of interactive search SQL/state. */
internal fun buildArticleRepositoryQuery(
    scope: ArticleSearchScope,
    query: ArticleQuery,
    searchColumns: List<String>,
): RoomRawQuery {
    require(query.limit > 0) { "Article limit must be positive" }
    val columns =
        searchColumns.filter { it in allSearchDomain.getValue(ARTICLE_TABLE_NAME) }.distinct()
    if (query.keywords.isNotEmpty() && columns.isEmpty()) {
        throw ArticleSearchFieldsDisabledException()
    }
    val arguments = mutableListOf<Any>()
    fun idSet(column: String, values: Collection<String>): String {
        if (values.isEmpty()) return "0"
        arguments.addAll(values)
        return "`$column` IN (${values.joinToString { "?" }})"
    }

    val filters = mutableListOf<String>()
    filters += when (scope) {
        ArticleSearchScope.All -> "1"
        is ArticleSearchScope.Restricted -> {
            require(scope.feedUrls.size + scope.articleIds.size <= SCOPE_CHUNK_SIZE)
            "(${idSet(ArticleBean.FEED_URL_COLUMN, scope.feedUrls)} OR " +
                    "${idSet(ArticleBean.ARTICLE_ID_COLUMN, scope.articleIds)})"
        }
    }
    query.withinArticleIds?.let { filters += idSet(ArticleBean.ARTICLE_ID_COLUMN, it) }
    if (query.excludeArticleIds.isNotEmpty()) {
        filters += "NOT (${idSet(ArticleBean.ARTICLE_ID_COLUMN, query.excludeArticleIds)})"
    }
    query.publishedFromMillis?.let {
        filters += "`${ArticleBean.DATE_COLUMN}` >= ?"
        arguments += it
    }
    query.publishedBeforeMillis?.let {
        filters += "`${ArticleBean.DATE_COLUMN}` < ?"
        arguments += it
    }
    query.read?.let {
        filters += "`${ArticleBean.IS_READ_COLUMN}` = ?"
        arguments += if (it) 1L else 0L
    }
    query.favorite?.let {
        filters += "`${ArticleBean.IS_FAVORITE_COLUMN}` = ?"
        arguments += if (it) 1L else 0L
    }
    if (query.keywords.isNotEmpty()) {
        val join = if (query.match == KeywordMatch.ALL) " AND " else " OR "
        filters += query.keywords.joinToString(join, "(", ")") { keyword ->
            val literal = "%${keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_")}%"
            columns.joinToString(" OR ", "(", ")") { column ->
                arguments += literal
                "`$column` LIKE ? ESCAPE '!'"
            }
        }
    }
    arguments += query.limit.toLong()
    require(arguments.size <= 999) { "Too many article query parameters" }
    return RoomRawQuery(
        "SELECT * FROM `$ARTICLE_TABLE_NAME` WHERE ${filters.joinToString(" AND ")} " +
                "ORDER BY `${ArticleBean.DATE_COLUMN}` DESC, `${ArticleBean.ARTICLE_ID_COLUMN}` ASC LIMIT ?"
    ) { statement ->
        arguments.forEachIndexed { index, value ->
            when (value) {
                is String -> statement.bindText(index + 1, value)
                is Long -> statement.bindLong(index + 1, value)
            }
        }
    }
}

// Leave room for other query filters and LIMIT on SQLite's 999 bind-parameter floor.
private const val SCOPE_CHUNK_SIZE = 600

internal fun ArticleSearchScope.chunks(): List<ArticleSearchScope> = when (this) {
    ArticleSearchScope.All -> listOf(this)
    is ArticleSearchScope.Restricted -> if (feedUrls.size + articleIds.size <= SCOPE_CHUNK_SIZE) {
        listOf(this)
    } else {
        feedUrls.chunked(SCOPE_CHUNK_SIZE).map {
            ArticleSearchScope.Restricted(feedUrls = it.toSet())
        } + articleIds.chunked(SCOPE_CHUNK_SIZE)
            .map { ArticleSearchScope.Restricted(articleIds = it.toSet()) }
    }
}
