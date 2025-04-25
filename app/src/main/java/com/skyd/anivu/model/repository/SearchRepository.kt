package com.skyd.anivu.model.repository

import android.database.DatabaseUtils
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.RoomRawQuery
import com.skyd.anivu.model.repository.BaseRepository
import com.skyd.anivu.config.allSearchDomain
import com.skyd.anivu.di.get
import com.skyd.anivu.ext.getOrDefault
import com.skyd.anivu.ext.splitByBlank
import com.skyd.anivu.model.bean.article.ARTICLE_TABLE_NAME
import com.skyd.anivu.model.bean.article.ArticleBean
import com.skyd.anivu.model.bean.article.ArticleWithFeed
import com.skyd.anivu.model.bean.feed.FEED_VIEW_NAME
import com.skyd.anivu.model.bean.feed.FeedViewBean
import com.skyd.anivu.model.db.dao.ArticleDao
import com.skyd.anivu.model.db.dao.FeedDao
import com.skyd.anivu.model.db.dao.SearchDomainDao
import com.skyd.anivu.model.preference.dataStore
import com.skyd.anivu.model.preference.search.IntersectSearchBySpacePreference
import com.skyd.anivu.model.preference.search.UseRegexSearchPreference
import com.skyd.anivu.model.repository.article.IArticleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import org.koin.core.annotation.Factory

@Factory(binds = [])
class SearchRepository(
    private val feedDao: FeedDao,
    private val articleDao: ArticleDao,
    private val articleRepo: IArticleRepository,
    private val pagingConfig: PagingConfig,
) : BaseRepository() {
    private val searchQuery = MutableStateFlow("")
    private val searchSortDateDesc = MutableStateFlow(true)

    fun updateQuery(query: String) {
        searchQuery.value = query
    }

    fun updateSort(dateDesc: Boolean) {
        searchSortDateDesc.value = dateDesc
    }

    fun listenSearchFeed(): Flow<PagingData<FeedViewBean>> = searchQuery.flatMapLatest { query ->
        val sql = genSql(tableName = FEED_VIEW_NAME, k = query)
        Pager(pagingConfig) { feedDao.getFeedPagingSource(sql) }.flow
    }.flowOn(Dispatchers.IO)

    fun listenSearchArticle(
        feedUrls: List<String>,
        groupIds: List<String>,
        articleIds: List<String>,
    ): Flow<PagingData<ArticleWithFeed>> = searchQuery.debounce(70).flatMapLatest { query ->
        val realFeedUrls = articleRepo.requestRealFeedUrls(
            feedUrls = feedUrls,
            groupIds = groupIds,
            articleIds = articleIds,
        ).first()
        val sql = genSql(
            tableName = ARTICLE_TABLE_NAME,
            k = query,
            leadingFilter = buildString {
                if (realFeedUrls.isEmpty()) {
                    append("(0 ")
                } else {
                    val feedUrlsStr = realFeedUrls.joinToString(", ") {
                        DatabaseUtils.sqlEscapeString(it)
                    }
                    append("(`${ArticleBean.FEED_URL_COLUMN}` IN ($feedUrlsStr) ")
                }
                if (articleIds.isNotEmpty()) {
                    val articleIdsStr = articleIds.joinToString(", ") {
                        DatabaseUtils.sqlEscapeString(it)
                    }
                    append("OR `${ArticleBean.ARTICLE_ID_COLUMN}` IN ($articleIdsStr) ")
                }
                append(")")
            },
            orderBy = {
                ArticleBean.DATE_COLUMN to if (searchSortDateDesc.value) "DESC" else "ASC"
            }
        )
        Pager(pagingConfig) { articleDao.getArticlePagingSource(sql) }.flow
    }.flowOn(Dispatchers.IO)

    class SearchRegexInvalidException(message: String?) : IllegalArgumentException(message)

    companion object {
        suspend fun genSql(
            tableName: String,
            k: String,
            useRegexSearch: Boolean = dataStore.getOrDefault(UseRegexSearchPreference),
            intersectSearchBySpace: Boolean = dataStore
                .getOrDefault(IntersectSearchBySpacePreference),
            useSearchDomain: suspend (table: String, column: String) -> Boolean = { table, column ->
                get<SearchDomainDao>().getSearchDomain(table, column)
            },
            leadingFilter: String = "1",
            leadingFilterLogicalConnective: String = "AND",
            limit: (() -> Pair<Int, Int>)? = null,
            orderBy: (() -> Pair<String, String>)? = null,
        ): RoomRawQuery {
            if (useRegexSearch) {
                // Check Regex format
                runCatching { k.toRegex() }.onFailure {
                    throw SearchRegexInvalidException(it.message)
                }
            }

            val sql = buildString {
                if (intersectSearchBySpace) {
                    // Split by blank
                    val keywords = k.splitByBlank().toSet()

                    keywords.forEachIndexed { i, s ->
                        if (i > 0) append("INTERSECT \n")
                        append(
                            "SELECT * FROM $tableName WHERE ${
                                getFilter(
                                    tableName = tableName,
                                    k = s,
                                    useRegexSearch = useRegexSearch,
                                    useSearchDomain = useSearchDomain,
                                    leadingFilter = leadingFilter,
                                    leadingFilterLogicalConnective = leadingFilterLogicalConnective,
                                )
                            } \n"
                        )
                    }
                } else {
                    append(
                        "SELECT * FROM $tableName WHERE ${
                            getFilter(
                                tableName = tableName,
                                k = k,
                                useRegexSearch = useRegexSearch,
                                useSearchDomain = useSearchDomain,
                                leadingFilter = leadingFilter,
                                leadingFilterLogicalConnective = leadingFilterLogicalConnective,
                            )
                        } \n"
                    )
                }
                if (limit != null) {
                    val (offset, count) = limit()
                    append("\nLIMIT $offset, $count")
                }
                if (orderBy != null) {
                    val (field, desc) = orderBy()
                    append("\nORDER BY $field $desc")
                }
            }
            return RoomRawQuery(sql)
        }

        private suspend fun getFilter(
            tableName: String,
            k: String,
            useRegexSearch: Boolean,
            useSearchDomain: suspend (tableName: String, columnName: String) -> Boolean,
            leadingFilter: String = "1",
            leadingFilterLogicalConnective: String = "AND",
        ): String {
            if (k.isBlank()) return leadingFilter

            var filter = "0"

            // Escape input text
            val keyword = if (useRegexSearch) {
                // Check Regex format
                runCatching { k.toRegex() }.onFailure {
                    throw SearchRegexInvalidException(it.message)
                }
                DatabaseUtils.sqlEscapeString(k)
            } else {
                DatabaseUtils.sqlEscapeString("%$k%")
            }

            val columns = allSearchDomain[tableName].orEmpty()
            for (column in columns) {
                if (!useSearchDomain(tableName, column)) {
                    continue
                }
                filter += if (useRegexSearch) {
                    " OR $column REGEXP $keyword"
                } else {
                    " OR $column LIKE $keyword"
                }
            }

            if (filter == "0") {
                filter += " OR 1"
            }
            filter = "$leadingFilter $leadingFilterLogicalConnective ($filter)"
            return filter
        }
    }

}