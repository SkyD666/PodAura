package com.skyd.anivu.model.repository

import android.database.DatabaseUtils
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.sqlite.db.SimpleSQLiteQuery
import com.skyd.anivu.appContext
import com.skyd.anivu.base.BaseRepository
import com.skyd.anivu.config.allSearchDomain
import com.skyd.anivu.ext.dataStore
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
import com.skyd.anivu.model.preference.search.IntersectSearchBySpacePreference
import com.skyd.anivu.model.preference.search.UseRegexSearchPreference
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class SearchRepository @Inject constructor(
    private val feedDao: FeedDao,
    private val articleDao: ArticleDao,
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

    fun listenSearchFeed(): Flow<PagingData<FeedViewBean>> {
        return searchQuery.flatMapLatest { query ->
            Pager(pagingConfig) {
                feedDao.getFeedPagingSource(
                    genSql(
                        tableName = FEED_VIEW_NAME, k = query
                    )
                )
            }.flow
        }.flowOn(Dispatchers.IO)
    }

    fun listenSearchArticle(
        feedUrls: List<String>,
        articleIds: List<String>,
    ): Flow<PagingData<ArticleWithFeed>> {
        return searchQuery.debounce(70).flatMapLatest { query ->
            Pager(pagingConfig) {
                articleDao.getArticlePagingSource(
                    genSql(
                    tableName = ARTICLE_TABLE_NAME,
                    k = query,
                    leadingFilter = buildString {
                        if (feedUrls.isEmpty()) {
                            append("(0 ")
                        } else {
                            val feedUrlsStr = feedUrls.joinToString(", ") {
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
                ))
            }.flow
        }.flowOn(Dispatchers.IO)
    }

    class SearchRegexInvalidException(message: String?) : IllegalArgumentException(message)

    companion object {
        @EntryPoint
        @InstallIn(SingletonComponent::class)
        interface SearchRepositoryEntryPoint {
            val searchDomainDao: SearchDomainDao
        }

        fun genSql(
            tableName: String,
            k: String,
            useRegexSearch: Boolean = appContext.dataStore.getOrDefault(UseRegexSearchPreference),
            // 是否使用多个关键字并集查询
            intersectSearchBySpace: Boolean = appContext.dataStore
                .getOrDefault(IntersectSearchBySpacePreference),
            useSearchDomain: (table: String, column: String) -> Boolean = { table, column ->
                EntryPointAccessors.fromApplication(
                    appContext, SearchRepositoryEntryPoint::class.java
                ).searchDomainDao.getSearchDomain(table, column)
            },
            leadingFilter: String = "1",
            leadingFilterLogicalConnective: String = "AND",
            limit: (() -> Pair<Int, Int>)? = null,
            orderBy: (() -> Pair<String, String>)? = null,
        ): SimpleSQLiteQuery {
            if (useRegexSearch) {
                // Check Regex format
                runCatching { k.toRegex() }.onFailure {
                    throw SearchRegexInvalidException(it.message)
                }
            }

            val sql = buildString {
                if (intersectSearchBySpace) {
                    // 以多个连续的空格/制表符/换行符分割
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
            return SimpleSQLiteQuery(sql)
        }

        private fun getFilter(
            tableName: String,
            k: String,
            useRegexSearch: Boolean,
            useSearchDomain: (tableName: String, columnName: String) -> Boolean,
            leadingFilter: String = "1",
            leadingFilterLogicalConnective: String = "AND",
        ): String {
            if (k.isBlank()) return leadingFilter

            var filter = "0"

            // 转义输入，防止SQL注入
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