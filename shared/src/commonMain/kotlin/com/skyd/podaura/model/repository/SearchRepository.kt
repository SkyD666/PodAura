package com.skyd.podaura.model.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.RoomRawQuery
import com.skyd.podaura.config.allSearchDomain
import com.skyd.podaura.di.get
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.ext.splitByBlank
import com.skyd.podaura.model.bean.article.ARTICLE_TABLE_NAME
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.model.bean.feed.FEED_VIEW_NAME
import com.skyd.podaura.model.bean.feed.FeedViewBean
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.db.dao.FeedDao
import com.skyd.podaura.model.db.dao.SearchDomainDao
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.preference.search.IntersectSearchBySpacePreference
import com.skyd.podaura.model.preference.search.UseRegexSearchPreference
import com.skyd.podaura.model.repository.article.IArticleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn

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
            leadingFilter = run {
                val args = mutableListOf<String>()
                buildString {
                    if (realFeedUrls.isEmpty()) {
                        append("(0 ")
                    } else {
                        val feedUrlsStr = realFeedUrls.joinToString(", ") { "?" }
                        append("(`${ArticleBean.Companion.FEED_URL_COLUMN}` IN ($feedUrlsStr) ")
                        args += realFeedUrls
                    }
                    if (articleIds.isNotEmpty()) {
                        val articleIdsStr = articleIds.joinToString(", ") { "?" }
                        append("OR `${ArticleBean.Companion.ARTICLE_ID_COLUMN}` IN ($articleIdsStr) ")
                        args += articleIds
                    }
                    append(")")
                } to args
            },
            orderBy = {
                ArticleBean.Companion.DATE_COLUMN to if (searchSortDateDesc.value) "DESC" else "ASC"
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
            leadingFilter: Pair<String, List<String>> = "1" to emptyList(),
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

            val args = mutableListOf<String>()
            val sql = buildString {
                if (intersectSearchBySpace) {
                    // Split by blank
                    val keywords = k.splitByBlank().toSet()

                    keywords.forEachIndexed { i, s ->
                        if (i > 0) append("INTERSECT \n")
                        val filter = getFilter(
                            tableName = tableName,
                            k = s,
                            useRegexSearch = useRegexSearch,
                            useSearchDomain = useSearchDomain,
                            leadingFilter = leadingFilter,
                            leadingFilterLogicalConnective = leadingFilterLogicalConnective,
                        )
                        args += filter.second
                        append("SELECT * FROM $tableName WHERE ${filter.first} \n")
                    }
                } else {
                    val filter = getFilter(
                        tableName = tableName,
                        k = k,
                        useRegexSearch = useRegexSearch,
                        useSearchDomain = useSearchDomain,
                        leadingFilter = leadingFilter,
                        leadingFilterLogicalConnective = leadingFilterLogicalConnective,
                    )
                    args += filter.second
                    append("SELECT * FROM $tableName WHERE ${filter.first} \n")
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
            return RoomRawQuery(sql) {
                args.forEachIndexed { index, text ->
                    it.bindText(index + 1, text)
                }
            }
        }

        private suspend fun getFilter(
            tableName: String,
            k: String,
            useRegexSearch: Boolean,
            useSearchDomain: suspend (tableName: String, columnName: String) -> Boolean,
            leadingFilter: Pair<String, List<String>> = "1" to emptyList(),
            leadingFilterLogicalConnective: String = "AND",
        ): Pair<String, List<String>> {
            if (k.isBlank()) return leadingFilter

            val args = mutableListOf<String>()
            var filter = "0"

            if (useRegexSearch) {   // Check Regex format
                runCatching { k.toRegex() }.onFailure {
                    throw SearchRegexInvalidException(it.message)
                }
            }

            val columns = allSearchDomain[tableName].orEmpty()
            for (column in columns) {
                if (!useSearchDomain(tableName, column)) {
                    continue
                }
                if (useRegexSearch) {
                    args += k
                    filter += " OR $column REGEXP ?"
                } else {
                    args += "%$k%"
                    filter += " OR $column LIKE ?"
                }
            }

            if (filter == "0") {
                filter += " OR 1"
            }
            filter = "${leadingFilter.first} $leadingFilterLogicalConnective ($filter)"
            return filter to leadingFilter.second + args
        }
    }

}