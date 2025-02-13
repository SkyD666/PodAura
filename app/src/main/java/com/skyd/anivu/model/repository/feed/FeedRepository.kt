package com.skyd.anivu.model.repository.feed

import android.net.Uri
import android.webkit.URLUtil
import com.skyd.anivu.appContext
import com.skyd.anivu.base.BaseRepository
import com.skyd.anivu.config.Const
import com.skyd.anivu.ext.copyTo
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.isLocal
import com.skyd.anivu.ext.isNetwork
import com.skyd.anivu.ext.put
import com.skyd.anivu.model.bean.feed.FeedBean
import com.skyd.anivu.model.bean.feed.FeedViewBean
import com.skyd.anivu.model.bean.group.GroupVo
import com.skyd.anivu.model.db.dao.ArticleDao
import com.skyd.anivu.model.db.dao.FeedDao
import com.skyd.anivu.model.db.dao.GroupDao
import com.skyd.anivu.model.preference.appearance.feed.FeedDefaultGroupExpandPreference
import com.skyd.anivu.model.preference.behavior.feed.HideMutedFeedPreference
import com.skyd.anivu.model.repository.RssHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.UUID
import javax.inject.Inject

class FeedRepository @Inject constructor(
    private val groupDao: GroupDao,
    private val feedDao: FeedDao,
    private val articleDao: ArticleDao,
    private val reorderGroupRepository: ReorderGroupRepository,
    private val rssHelper: RssHelper,
) : BaseRepository() {
    fun requestGroupAnyList(): Flow<List<Any>> = combine(
        groupDao.getGroupWithFeeds(),
        feedDao.getFeedsInDefaultGroup(),
        appContext.dataStore.data.map {
            it[HideMutedFeedPreference.key] ?: HideMutedFeedPreference.default
        }.distinctUntilChanged()
    ) { groupList, defaultFeeds, hideMute ->
        mutableListOf<Any>().apply {
            add(GroupVo.DefaultGroup)
            addAll(defaultFeeds.run { if (hideMute) filter { !it.feed.mute } else this })
            reorderGroupRepository.sortGroupWithFeed(groupList).forEach { group ->
                add(group.group.toVo())
                addAll(group.feeds.run { if (hideMute) filter { !it.feed.mute } else this })
            }
        }
    }.flowOn(Dispatchers.IO)

    fun requestAllFeedList(): Flow<List<FeedBean>> = feedDao.getAllFeedList()
        .map { list -> list.sortedBy { it.title } }
        .flowOn(Dispatchers.IO)

    fun clearGroupArticles(groupId: String): Flow<Int> = flow {
        val realGroupId = if (groupId == GroupVo.DEFAULT_GROUP_ID) null else groupId
        emit(articleDao.deleteArticlesInGroup(realGroupId))
    }

    suspend fun deleteGroup(groupId: String): Flow<Int> {
        return if (groupId == GroupVo.DEFAULT_GROUP_ID) flowOf(0)
        else flowOf(groupDao.removeGroupWithFeed(groupId)).flowOn(Dispatchers.IO)
    }

    fun renameGroup(groupId: String, name: String): Flow<GroupVo> {
        return if (groupId == GroupVo.DEFAULT_GROUP_ID) flow {
            emit(GroupVo.DefaultGroup)
        } else flow {
            groupDao.renameGroup(groupId, name)
            emit(groupDao.getGroupById(groupId).toVo())
        }.flowOn(Dispatchers.IO)
    }

    suspend fun moveGroupFeedsTo(fromGroupId: String, toGroupId: String): Flow<Int> {
        val realFromGroupId = if (fromGroupId == GroupVo.DEFAULT_GROUP_ID) null else fromGroupId
        val realToGroupId = if (toGroupId == GroupVo.DEFAULT_GROUP_ID) null else toGroupId
        return flowOf(groupDao.moveGroupFeedsTo(realFromGroupId, realToGroupId))
            .flowOn(Dispatchers.IO)
    }

    fun getFeedViewsByUrls(urls: List<String>) = flow {
        emit(feedDao.getFeedsIn(urls))
    }.flowOn(Dispatchers.IO)

    fun getFeedViewsByGroupId(groupId: String?) = flow {
        emit(feedDao.getFeedsByGroupId(groupId))
    }.flowOn(Dispatchers.IO)

    fun setFeed(
        url: String,
        groupId: String?,
        nickname: String?,
    ): Flow<FeedViewBean> = flow {
        val realNickname = if (nickname.isNullOrBlank()) null else nickname
        val realGroupId =
            if (groupId.isNullOrBlank() || groupId == GroupVo.DEFAULT_GROUP_ID) null else groupId
        val feedWithArticleBean = rssHelper.searchFeed(url = url).run {
            copy(
                feed = feed.copy(
                    groupId = realGroupId,
                    nickname = realNickname,
                )
            )
        }
        feedDao.setFeedWithArticle(feedWithArticleBean)
        emit(feedDao.getFeed(url))
    }.flowOn(Dispatchers.IO)

    fun editFeedUrl(
        oldUrl: String,
        newUrl: String,
    ): Flow<FeedViewBean> = flow {
        val oldFeed = feedDao.getFeed(oldUrl)
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
            newFeed = feedDao.getFeed(newUrl)
        }
        emit(newFeed)
    }.flowOn(Dispatchers.IO)

    fun editFeedNickname(
        url: String,
        nickname: String?,
    ): Flow<FeedViewBean> = flow {
        val realNickname = if (nickname.isNullOrBlank()) null else nickname
        feedDao.updateFeed(feedDao.getFeed(url).feed.copy(nickname = realNickname))
        emit(feedDao.getFeed(url))
    }.flowOn(Dispatchers.IO)

    fun editFeedGroup(
        url: String,
        groupId: String?,
    ): Flow<FeedViewBean> = flow {
        val realGroupId =
            if (groupId.isNullOrBlank() || groupId == GroupVo.DEFAULT_GROUP_ID) null
            else groupId

        feedDao.updateFeed(feedDao.getFeed(url).feed.copy(groupId = realGroupId))
        emit(feedDao.getFeed(url))
    }.flowOn(Dispatchers.IO)

    fun editFeedCustomDescription(
        url: String,
        customDescription: String?,
    ): Flow<FeedViewBean> = flow {
        feedDao.updateFeed(feedDao.getFeed(url).feed.copy(customDescription = customDescription))
        emit(feedDao.getFeed(url))
    }.flowOn(Dispatchers.IO)

    fun editFeedCustomIcon(
        url: String,
        customIcon: Uri?,
    ): Flow<FeedViewBean> = flow {
        var filePath: String? = null
        if (customIcon != null) {
            if (customIcon.isLocal()) {
                customIcon.copyTo(
                    File(Const.FEED_ICON_DIR, UUID.randomUUID().toString()).apply {
                        filePath = path
                    }
                )
            } else if (customIcon.isNetwork()) {
                filePath = customIcon.toString()
            }
        }
        val oldFeed = feedDao.getFeed(url)
        oldFeed.feed.customIcon?.let { icon -> tryDeleteFeedIconFile(icon) }
        feedDao.updateFeed(oldFeed.feed.copy(customIcon = filePath))
        emit(feedDao.getFeed(url))
    }.flowOn(Dispatchers.IO)

    fun editFeedSortXmlArticlesOnUpdate(
        url: String,
        sort: Boolean,
    ): Flow<FeedViewBean> = flow {
        feedDao.updateFeedSortXmlArticlesOnUpdate(feedUrl = url, sort = sort)
        emit(feedDao.getFeed(url))
    }.flowOn(Dispatchers.IO)

    fun removeFeed(url: String): Flow<Int> {
        return flow {
            feedDao.getFeed(url).feed.customIcon?.let { icon -> tryDeleteFeedIconFile(icon) }
            emit(feedDao.removeFeed(url))
        }.flowOn(Dispatchers.IO)
    }

    fun clearFeedArticles(url: String): Flow<Int> = flow {
        emit(articleDao.deleteArticleInFeed(url))
    }.flowOn(Dispatchers.IO)

    fun createGroup(group: GroupVo): Flow<Unit> = flow {
        if (groupDao.containsByName(group.name) == 0) {
            emit(groupDao.setGroup(group.toPo()))
        } else {
            emit(Unit)
        }
    }.flowOn(Dispatchers.IO)

    fun changeGroupExpanded(groupId: String?, expanded: Boolean): Flow<Unit> {
        return flow {
            if (groupId == null || groupId == GroupVo.DEFAULT_GROUP_ID) {
                appContext.dataStore.put(
                    FeedDefaultGroupExpandPreference.key,
                    value = expanded,
                )
            } else {
                groupDao.changeGroupExpanded(groupId, expanded)
            }
            emit(Unit)
        }.flowOn(Dispatchers.IO)
    }

    fun readAllInGroup(groupId: String?): Flow<Int> = flow {
        val realGroupId = if (groupId == GroupVo.DEFAULT_GROUP_ID) null else groupId
        emit(articleDao.readAllInGroup(realGroupId))
    }.flowOn(Dispatchers.IO)

    fun readAllInFeed(feedUrl: String): Flow<Int> = flow {
        emit(articleDao.readAllInFeed(feedUrl))
    }.flowOn(Dispatchers.IO)

    fun muteFeed(feedUrl: String, mute: Boolean): Flow<Int> = flow {
        emit(feedDao.muteFeed(feedUrl, mute))
    }.flowOn(Dispatchers.IO)

    fun muteFeedsInGroup(groupId: String?, mute: Boolean): Flow<Int> = flow {
        val realGroupId = if (groupId == GroupVo.DefaultGroup.groupId) null else groupId
        emit(feedDao.muteFeedsInGroup(realGroupId, mute))
    }.flowOn(Dispatchers.IO)
}

fun tryDeleteFeedIconFile(path: String?) {
    if (path != null && !URLUtil.isNetworkUrl(path)) {
        try {
            File(path).delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}