package com.skyd.podaura.model.repository.feed

import com.skyd.podaura.model.bean.feed.FeedBean
import kotlinx.coroutines.flow.Flow

interface IFeedRepository {
    fun requestAllFeedList(): Flow<List<FeedBean>>

    fun muteFeed(feedUrl: String, mute: Boolean): Flow<Int>
}