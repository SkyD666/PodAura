package com.skyd.podaura.model.repository.feed.sheet

import androidx.paging.PagingData
import com.skyd.podaura.model.bean.feed.FeedViewBean
import com.skyd.podaura.model.bean.group.GroupVo
import kotlinx.coroutines.flow.Flow

interface IFeedSheetRepository {
    fun getFeed(feedUrl: String): Flow<FeedViewBean>

    fun editFeedUrl(oldUrl: String, newUrl: String): Flow<FeedViewBean>

    fun editFeedNickname(url: String, nickname: String?): Flow<FeedViewBean>

    fun editFeedGroup(url: String, groupId: String?): Flow<FeedViewBean>

    fun editFeedCustomDescription(url: String, customDescription: String?): Flow<FeedViewBean>

    fun editFeedCustomIcon(url: String, customIcon: String?): Flow<FeedViewBean>

    fun editFeedSortXmlArticlesOnUpdate(url: String, sort: Boolean): Flow<FeedViewBean>

    fun removeFeed(url: String): Flow<Int>

    fun clearFeedArticles(url: String): Flow<Int>

    fun readAllInFeed(feedUrl: String): Flow<Int>

    fun muteFeed(feedUrl: String, mute: Boolean): Flow<Int>

    fun getFeedViewsByUrls(urls: List<String>): Flow<List<FeedViewBean>>

    fun createGroup(group: GroupVo): Flow<Unit>

    fun requestGroups(): Flow<PagingData<GroupVo>>
}