package com.skyd.podaura.model.repository.feed

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.skyd.fundation.ext.deleteRecursively
import com.skyd.podaura.ext.flowOf
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.ext.isHttpOrHttps
import com.skyd.podaura.ext.put
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.bean.feed.FeedViewBean
import com.skyd.podaura.model.bean.group.GroupVo
import com.skyd.podaura.model.bean.group.groupfeed.GroupOrFeedBean
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.db.dao.FeedDao
import com.skyd.podaura.model.db.dao.GroupDao
import com.skyd.podaura.model.db.dao.playlist.PlaylistDao.Companion.ORDER_DELTA
import com.skyd.podaura.model.preference.appearance.feed.FeedDefaultGroupExpandPreference
import com.skyd.podaura.model.preference.behavior.feed.HideEmptyDefaultPreference
import com.skyd.podaura.model.preference.behavior.feed.HideMutedFeedPreference
import com.skyd.podaura.model.preference.data.delete.KeepFavoriteArticlesPreference
import com.skyd.podaura.model.preference.data.delete.KeepPlaylistArticlesPreference
import com.skyd.podaura.model.preference.data.delete.KeepUnreadArticlesPreference
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.repository.BaseRepository
import com.skyd.podaura.model.repository.feed.sheet.IFeedSheetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.io.files.Path

class FeedRepository(
    private val groupDao: GroupDao,
    private val feedDao: FeedDao,
    private val articleDao: ArticleDao,
    private val rssHelper: RssHelper,
    private val pagingConfig: PagingConfig,
    private val feedSheetRepository: IFeedSheetRepository,
) : BaseRepository(), IFeedRepository, IFeedSheetRepository by feedSheetRepository {
    fun allGroupCollapsed(): Flow<Boolean> = combine(
        groupDao.existsExpandedGroup(),
        dataStore.flowOf(FeedDefaultGroupExpandPreference),
    ) { existsExpandedGroup, defaultGroupExpanded ->
        existsExpandedGroup == 0 && !defaultGroupExpanded
    }.flowOn(Dispatchers.IO)

    fun requestGroupAnyPaging(): Flow<PagingData<Any>> = dataStore.run {
        combine(
            flowOf(FeedDefaultGroupExpandPreference),
            flowOf(HideEmptyDefaultPreference),
            flowOf(HideMutedFeedPreference),
        ) { defaultGroupExpand, hideEmptyDefault, hideMutedFeed ->
            listOf(defaultGroupExpand, hideEmptyDefault, hideMutedFeed)
        }
    }.flatMapLatest { (defaultGroupExpand, hideEmptyDefault, hideMutedFeed) ->
        Pager(pagingConfig) {
            groupDao.getGroupsAndFeeds(
                defaultGroupIsExpanded = defaultGroupExpand,
                hideEmptyDefaultGroup = hideEmptyDefault,
                hideMutedFeed = hideMutedFeed,
            )
        }.flow.map { pagingData ->
            pagingData.map<GroupOrFeedBean, Any> { entity ->
                if (entity.group == null && entity.feed == null) {
                    GroupVo.DefaultGroup
                } else if (entity.group != null) {
                    entity.group.toVo()
                } else {
                    entity.feed!!
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    fun requestGroupAnyList(): Flow<List<Any>> = combine(
        groupDao.getGroupWithFeeds(),
        feedDao.getFeedsInDefaultGroup(),
        dataStore.flowOf(HideMutedFeedPreference),
    ) { groupList, defaultFeeds, hideMute ->
        mutableListOf<Any>().apply {
            add(GroupVo.DefaultGroup)
            addAll(defaultFeeds.run { if (hideMute) filter { !it.feed.mute } else this })
            groupList.forEach { group ->
                add(group.group.toVo())
                addAll(group.feeds.run { if (hideMute) filter { !it.feed.mute } else this })
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun requestAllFeedList(): Flow<List<FeedBean>> = feedDao.getAllFeedList()
        .map { list -> list.sortedBy { it.title } }
        .flowOn(Dispatchers.IO)

    fun clearGroupArticles(groupId: String): Flow<Int> = flow {
        val realGroupId = if (groupId == GroupVo.DEFAULT_GROUP_ID) null else groupId
        val count = with(dataStore) {
            articleDao.deleteArticlesInGroup(
                groupId = realGroupId,
                keepPlaylistArticles = getOrDefault(KeepPlaylistArticlesPreference),
                keepUnread = getOrDefault(KeepUnreadArticlesPreference),
                keepFavorite = getOrDefault(KeepFavoriteArticlesPreference),
            )
        }
        emit(count)
    }

    fun deleteGroup(groupId: String): Flow<Int> = flow {
        if (groupId == GroupVo.DEFAULT_GROUP_ID) emit(0)
        else {
            feedDao.getFeedsInGroup(listOf(groupId)).forEach {
                it.feed.customIcon?.let { icon -> tryDeleteFeedIconFile(icon) }
            }
            emit(groupDao.removeGroupWithFeed(groupId))
        }
    }.flowOn(Dispatchers.IO)

    fun renameGroup(groupId: String, name: String): Flow<GroupVo> = flow {
        if (groupId == GroupVo.DEFAULT_GROUP_ID) {
            emit(GroupVo.DefaultGroup)
        } else {
            groupDao.renameGroup(groupId, name)
            emit(groupDao.getGroupById(groupId).toVo())
        }
    }.flowOn(Dispatchers.IO)

    fun moveGroupFeedsTo(fromGroupId: String, toGroupId: String): Flow<Int> = flow {
        val realFromGroupId = if (fromGroupId == GroupVo.DEFAULT_GROUP_ID) null else fromGroupId
        val realToGroupId = if (toGroupId == GroupVo.DEFAULT_GROUP_ID) null else toGroupId
        emit(groupDao.moveGroupFeedsTo(realFromGroupId, realToGroupId))
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
        val oldFeed = feedDao.getFeed(url)
        val feedWithArticleBean = rssHelper.searchFeed(url = url).run {
            copy(
                feed = feed.copy(
                    groupId = realGroupId,
                    nickname = realNickname,
                    orderPosition = oldFeed?.orderPosition
                        ?: (feedDao.getMaxOrder(realGroupId) + ORDER_DELTA)
                )
            )
        }
        feedDao.setFeedWithArticle(feedWithArticleBean)
        emit(feedDao.getFeedView(url))
    }.flowOn(Dispatchers.IO)

    fun changeGroupExpanded(groupId: String?, expanded: Boolean): Flow<Unit> = flow {
        if (groupId == null || groupId == GroupVo.DEFAULT_GROUP_ID) {
            dataStore.put(
                FeedDefaultGroupExpandPreference.key,
                value = expanded,
            )
        } else {
            groupDao.changeGroupExpanded(groupId, expanded)
        }
        emit(Unit)
    }.flowOn(Dispatchers.IO)

    fun readAllInGroup(groupId: String?): Flow<Int> = flow {
        val realGroupId = if (groupId == GroupVo.DEFAULT_GROUP_ID) null else groupId
        emit(articleDao.readAllInGroup(realGroupId))
    }.flowOn(Dispatchers.IO)

    fun muteFeedsInGroup(groupId: String?, mute: Boolean): Flow<Int> = flow {
        val realGroupId = if (groupId == GroupVo.DefaultGroup.groupId) null else groupId
        emit(feedDao.muteFeedsInGroup(realGroupId, mute))
    }.flowOn(Dispatchers.IO)

    fun collapseAllGroup(collapse: Boolean): Flow<Int> = flow {
        dataStore.put(FeedDefaultGroupExpandPreference.key, !collapse)
        emit(groupDao.collapseAllGroup(collapse) + 1)
    }.flowOn(Dispatchers.IO)
}

fun tryDeleteFeedIconFile(path: String?) {
    if (path != null && !path.isHttpOrHttps()) {
        try {
            Path(path).deleteRecursively()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
