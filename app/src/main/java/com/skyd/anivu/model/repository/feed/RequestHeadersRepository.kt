package com.skyd.anivu.model.repository.feed

import com.skyd.anivu.model.repository.BaseRepository
import com.skyd.anivu.model.bean.feed.FeedBean
import com.skyd.anivu.model.db.dao.FeedDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.koin.core.annotation.Factory

@Factory(binds = [])
class RequestHeadersRepository(
    private val feedDao: FeedDao,
) : BaseRepository() {
    fun getFeedHeaders(feedUrl: String): Flow<FeedBean.RequestHeaders?> =
        feedDao.getFeedHeaders(feedUrl).flowOn(Dispatchers.IO)

    fun updateFeedHeaders(feedUrl: String, headers: FeedBean.RequestHeaders): Flow<Unit> = flow {
        emit(feedDao.updateFeedHeaders(feedUrl, headers))
    }.flowOn(Dispatchers.IO)
}