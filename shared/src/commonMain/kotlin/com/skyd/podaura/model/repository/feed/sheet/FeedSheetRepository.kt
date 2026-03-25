package com.skyd.podaura.model.repository.feed.sheet

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.skyd.fundation.config.Const
import com.skyd.fundation.config.FEED_ICON_DIR
import com.skyd.podaura.ext.asPlatformFile
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.ext.isLocalFile
import com.skyd.podaura.ext.isNetworkUrl
import com.skyd.podaura.model.bean.feed.FeedViewBean
import com.skyd.podaura.model.bean.group.GroupVo
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.db.dao.FeedDao
import com.skyd.podaura.model.db.dao.GroupDao
import com.skyd.podaura.model.db.dao.playlist.PlaylistDao.Companion.ORDER_DELTA
import com.skyd.podaura.model.preference.data.delete.KeepFavoriteArticlesPreference
import com.skyd.podaura.model.preference.data.delete.KeepPlaylistArticlesPreference
import com.skyd.podaura.model.preference.data.delete.KeepUnreadArticlesPreference
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.repository.feed.RssHelper
import com.skyd.podaura.model.repository.feed.tryDeleteFeedIconFile
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.copyTo
import io.github.vinceglb.filekit.utils.div
import io.github.vinceglb.filekit.utils.toPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlin.uuid.Uuid

class FeedSheetRepository(
    private val groupDao: GroupDao,
    private val feedDao: FeedDao,
    private val articleDao: ArticleDao,
    private val rssHelper: RssHelper,
    private val pagingConfig: PagingConfig,
) : IFeedSheetRepository {
    override fun getFeed(feedUrl: String): Flow<FeedViewBean> = flow {
        emit(feedDao.getFeedView(feedUrl))
    }

    override fun editFeedUrl(
        oldUrl: String,
        newUrl: String
    ): Flow<FeedViewBean> = flow {
        val oldFeed = feedDao.getFeedView(oldUrl)
        var newFeed = oldFeed
        if (oldUrl != newUrl) {
            val feedWithArticleBean = rssHelper.searchFeed(url = newUrl).run {
                copy(
                    feed = feed.copy(
                        groupId = oldFeed.feed.groupId,
                        nickname = oldFeed.feed.nickname,
                        customDescription = oldFeed.feed.customDescription,
                        customIcon = oldFeed.feed.customIcon,
                    )
                )
            }
            feedDao.removeFeed(oldUrl)
            feedDao.setFeedWithArticle(feedWithArticleBean)
            newFeed = feedDao.getFeedView(newUrl)
        }
        emit(newFeed)
    }.flowOn(Dispatchers.IO)

    override fun editFeedNickname(
        url: String,
        nickname: String?
    ): Flow<FeedViewBean> = flow {
        val realNickname = if (nickname.isNullOrBlank()) null else nickname
        feedDao.updateFeed(feedDao.getFeedView(url).feed.copy(nickname = realNickname))
        emit(feedDao.getFeedView(url))
    }.flowOn(Dispatchers.IO)

    override fun editFeedGroup(
        url: String,
        groupId: String?
    ): Flow<FeedViewBean> = flow {
        val realGroupId = if (groupId.isNullOrBlank() || groupId == GroupVo.DEFAULT_GROUP_ID) {
            null
        } else {
            groupId
        }
        feedDao.updateFeed(
            feedDao.getFeedView(url).feed.copy(
                groupId = realGroupId,
                orderPosition = feedDao.getMaxOrder(realGroupId) + ORDER_DELTA,
            )
        )
        emit(feedDao.getFeedView(url))
    }.flowOn(Dispatchers.IO)

    override fun editFeedCustomDescription(
        url: String,
        customDescription: String?
    ): Flow<FeedViewBean> = flow {
        feedDao.updateFeed(feedDao.getFeedView(url).feed.copy(customDescription = customDescription))
        emit(feedDao.getFeedView(url))
    }.flowOn(Dispatchers.IO)

    override fun editFeedCustomIcon(
        url: String,
        customIcon: String?
    ): Flow<FeedViewBean> = flow {
        var filePath: String? = null
        if (customIcon != null) {
            if (customIcon.isLocalFile()) {
                val dest = PlatformFile(Const.FEED_ICON_DIR.toPath() / Uuid.random().toString())
                filePath = dest.toString()
                customIcon.asPlatformFile().copyTo(dest)
            } else if (customIcon.isNetworkUrl()) {
                filePath = customIcon
            }
        }
        val oldFeed = feedDao.getFeedView(url)
        oldFeed.feed.customIcon?.let { icon -> tryDeleteFeedIconFile(icon) }
        feedDao.updateFeed(oldFeed.feed.copy(customIcon = filePath))
        emit(feedDao.getFeedView(url))
    }.flowOn(Dispatchers.IO)

    override fun editFeedSortXmlArticlesOnUpdate(
        url: String,
        sort: Boolean
    ): Flow<FeedViewBean> = flow {
        feedDao.updateFeedSortXmlArticlesOnUpdate(feedUrl = url, sort = sort)
        emit(feedDao.getFeedView(url))
    }.flowOn(Dispatchers.IO)

    override fun removeFeed(url: String): Flow<Int> = flow {
        feedDao.getFeedView(url).feed.customIcon?.let { icon -> tryDeleteFeedIconFile(icon) }
        emit(feedDao.removeFeed(url))
    }.flowOn(Dispatchers.IO)

    override fun clearFeedArticles(url: String): Flow<Int> = flow {
        val count = with(dataStore) {
            articleDao.deleteArticleInFeed(
                feedUrl = url,
                keepPlaylistArticles = getOrDefault(KeepPlaylistArticlesPreference),
                keepUnread = getOrDefault(KeepUnreadArticlesPreference),
                keepFavorite = getOrDefault(KeepFavoriteArticlesPreference),
            )
        }
        emit(count)
    }.flowOn(Dispatchers.IO)

    override fun readAllInFeed(feedUrl: String): Flow<Int> = flow {
        emit(articleDao.readAllInFeed(feedUrl))
    }.flowOn(Dispatchers.IO)

    override fun muteFeed(
        feedUrl: String,
        mute: Boolean
    ): Flow<Int> = flow {
        emit(feedDao.muteFeed(feedUrl, mute))
    }.flowOn(Dispatchers.IO)

    override fun getFeedViewsByUrls(urls: List<String>): Flow<List<FeedViewBean>> = flow {
        emit(feedDao.getFeedsIn(urls))
    }.flowOn(Dispatchers.IO)

    override fun createGroup(group: GroupVo): Flow<Unit> = flow {
        if (groupDao.containsByName(group.name) == 0) {
            emit(
                groupDao.setGroup(
                    group.toPo(orderPosition = groupDao.getMaxOrder() + GroupDao.ORDER_DELTA)
                )
            )
        } else {
            emit(Unit)
        }
    }.flowOn(Dispatchers.IO)

    override fun requestGroups(): Flow<PagingData<GroupVo>> = Pager(pagingConfig) {
        groupDao.getGroups()
    }.flow.map { pagingData -> pagingData.map { it.toVo() } }.flowOn(Dispatchers.IO)
}