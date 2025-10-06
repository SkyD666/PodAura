package com.skyd.podaura.model.repository.article

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.RoomRawQuery
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.model.bean.article.ARTICLE_TABLE_NAME
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.bean.group.GroupVo
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.db.dao.FeedDao
import com.skyd.podaura.model.preference.data.delete.KeepFavoriteArticlesPreference
import com.skyd.podaura.model.preference.data.delete.KeepPlaylistArticlesPreference
import com.skyd.podaura.model.preference.data.delete.KeepUnreadArticlesPreference
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.repository.BaseRepository
import com.skyd.podaura.model.repository.feed.RssHelper
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jetbrains.compose.resources.getString
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.rss_update_failed
import kotlin.coroutines.cancellation.CancellationException


class ArticleRepository(
    private val feedDao: FeedDao,
    private val articleDao: ArticleDao,
    private val rssHelper: RssHelper,
    private val pagingConfig: PagingConfig,
) : BaseRepository(), IArticleRepository {
    private val filterMask: MutableStateFlow<Int> = MutableStateFlow(0)

    fun requestFilterMask(): Flow<Int> = filterMask.asStateFlow()

    fun isOnlySingleFeed(
        feedUrls: List<String>,
        groupIds: List<String>,
        articleIds: List<String>,
    ) = feedUrls.size == 1 && groupIds.isEmpty() && articleIds.isEmpty()

    fun updateFilterMask(
        feedUrls: List<String>,
        groupIds: List<String>,
        articleIds: List<String>,
        filterMask: Int,
    ): Flow<Int> = flow {
        this@ArticleRepository.filterMask.emit(filterMask)
        if (isOnlySingleFeed(feedUrls, groupIds, articleIds)) {
            emit(feedDao.updateFilterMask(url = feedUrls.first(), filterMask = filterMask))
        } else {
            emit(0)
        }
    }

    override fun requestRealFeedUrls(
        feedUrls: List<String>,
        groupIds: List<String>,
        articleIds: List<String>,
    ): Flow<List<String>> = flow {
        val realFeedUrls = if (feedUrls.isEmpty() && groupIds.isEmpty() && articleIds.isEmpty()) {
            feedDao.getAllFeedUrl()
        } else {
            val realGroupIds =
                groupIds.filter { it.isNotEmpty() && GroupVo.DefaultGroup.groupId != it }
            val hasDefault = realGroupIds.size != groupIds.size
            buildList {
                addAll(feedUrls)
                if (realGroupIds.isNotEmpty()) addAll(feedDao.getFeedUrlsInGroup(realGroupIds))
                if (hasDefault) addAll(feedDao.getFeedUrlsInDefaultGroup())
            }
        }
        emit(realFeedUrls)
    }.flowOn(Dispatchers.IO)

    fun requestArticleList(
        feedUrls: List<String>,
        groupIds: List<String>,
        articleIds: List<String>,
    ): Flow<PagingData<ArticleWithFeed>> = flow {
        if (isOnlySingleFeed(feedUrls, groupIds, articleIds)) {
            filterMask.emit(feedDao.getFilterMask(feedUrls.first()))
        }
        emit(Unit)
    }.flatMapLatest { filterMask }.flatMapLatest { filterMask ->
        val favorite = FeedBean.parseFilterMaskToFavorite(filterMask)
        val read = FeedBean.parseFilterMaskToRead(filterMask)
        val sortBy = FeedBean.parseFilterMaskToSort(filterMask)
        val realFeedUrls = requestRealFeedUrls(
            feedUrls = feedUrls,
            groupIds = groupIds,
            articleIds = articleIds,
        ).first()
        Pager(pagingConfig) {
            articleDao.getArticlePagingSource(
                genSql(
                    feedUrls = realFeedUrls.distinct(),
                    articleIds = articleIds,
                    isFavorite = favorite,
                    isRead = read,
                    orderBy = sortBy,
                )
            )
        }.flow
    }.flowOn(Dispatchers.IO)

    override fun refreshGroupArticles(groupId: String?, full: Boolean): Flow<Unit> = flow {
        val realGroupId = if (groupId == GroupVo.DEFAULT_GROUP_ID) null else groupId
        emit(feedDao.getFeedsByGroupId(realGroupId).map { it.feed.url })
    }.flatMapConcat {
        refreshArticleList(feedUrls = it, full = full)
    }.flowOn(Dispatchers.IO)

    class RefreshFeedsException(msg: String) : RuntimeException(msg)

    override fun refreshArticleList(feedUrls: List<String>, full: Boolean): Flow<Unit> = flow {
        coroutineScope {
            val requests = mutableListOf<Deferred<Unit>>()
            val failMsg = mutableListOf<Pair<String, String>>()
            val semaphore = Semaphore(5)
            feedUrls.forEach { feedUrl ->
                requests += async {
                    semaphore.withPermit {
                        val articleBeanList = runCatching {
                            rssHelper.queryRssXml(
                                feed = feedDao.getFeedView(feedUrl).feed,
                                full = full,
                                latestLink = articleDao.queryLatestByFeedUrl(feedUrl)?.link,
                            )?.also { feedWithArticle ->
                                feedDao.updateFeed(feedWithArticle.feed)
                            }?.articles
                        }.onFailure { e ->
                            if (e !is CancellationException) {
                                e.printStackTrace()
                                failMsg += (feedUrl to e.message.orEmpty())
                            }
                        }.getOrNull()

                        if (articleBeanList.isNullOrEmpty()) return@async

                        articleDao.insertListIfNotExist(articleBeanList.map { articleWithEnclosure ->
                            if (articleWithEnclosure.article.feedUrl != feedUrl) {
                                articleWithEnclosure.copy(
                                    article = articleWithEnclosure.article.copy(feedUrl = feedUrl)
                                )
                            } else articleWithEnclosure
                        })
                    }
                }
            }
            requests.awaitAll()
            if (failMsg.isNotEmpty()) {
                throw RefreshFeedsException(
                    getString(
                        Res.string.rss_update_failed, failMsg.size,
                        failMsg.joinToString(
                            separator = "\n",
                            limit = 10,
                            transform = { "-${it.first}" }
                        ),
                    )
                )
            }
            emit(Unit)
        }
    }.flowOn(Dispatchers.IO)

    override fun favoriteArticle(articleId: String, favorite: Boolean): Flow<Unit> = flow {
        emit(articleDao.favoriteArticle(articleId, favorite))
    }.flowOn(Dispatchers.IO)

    override fun readArticle(articleId: String, read: Boolean): Flow<Unit> = flow {
        emit(articleDao.readArticle(articleId, read))
    }.flowOn(Dispatchers.IO)

    override fun deleteArticle(articleId: String): Flow<Int> = flow {
        with(dataStore) {
            emit(
                articleDao.deleteArticle(
                    articleId,
                    keepPlaylistArticles = getOrDefault(KeepPlaylistArticlesPreference),
                    keepUnread = getOrDefault(KeepUnreadArticlesPreference),
                    keepFavorite = getOrDefault(KeepFavoriteArticlesPreference),
                )
            )
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        fun genSql(
            feedUrls: List<String>,
            articleIds: List<String>,
            isFavorite: Boolean?,
            isRead: Boolean?,
            orderBy: FeedBean.SortBy,
        ): RoomRawQuery {
            val args = mutableListOf<String>()
            val sql = buildString {
                append("SELECT DISTINCT * FROM `$ARTICLE_TABLE_NAME` WHERE 1 ")
                if (isFavorite != null) {
                    append("AND `${ArticleBean.IS_FAVORITE_COLUMN}` = ${if (isFavorite) 1 else 0} ")
                }
                if (isRead != null) {
                    append("AND `${ArticleBean.IS_READ_COLUMN}` = ${if (isRead) 1 else 0} ")
                }
                if (feedUrls.isEmpty()) {
                    append("AND (0 ")
                } else {
                    val feedUrlsStr = feedUrls.joinToString(", ") { "?" }
                    append("AND (`${ArticleBean.FEED_URL_COLUMN}` IN ($feedUrlsStr) ")
                    args += feedUrls
                }
                if (articleIds.isNotEmpty()) {
                    val articleIdsStr = articleIds.joinToString(", ") { "?" }
                    append("OR `${ArticleBean.ARTICLE_ID_COLUMN}` IN ($articleIdsStr) ")
                    args += articleIds
                }
                append(") ")
                val ascOrDesc = if (orderBy.asc) "ASC" else "DESC"
                val orderField = when (orderBy) {
                    is FeedBean.SortBy.Date -> ArticleBean.DATE_COLUMN
                    is FeedBean.SortBy.Title -> ArticleBean.TITLE_COLUMN
                }
                append("\nORDER BY `$orderField` $ascOrDesc")
            }
            return RoomRawQuery(sql) {
                args.forEachIndexed { index, text ->
                    it.bindText(index + 1, text)
                }
            }
        }
    }
}