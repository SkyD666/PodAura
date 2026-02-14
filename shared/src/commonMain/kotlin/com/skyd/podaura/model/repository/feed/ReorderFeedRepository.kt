package com.skyd.podaura.model.repository.feed

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.skyd.podaura.model.bean.feed.FeedViewBean
import com.skyd.podaura.model.bean.group.GroupVo
import com.skyd.podaura.model.db.dao.FeedDao
import com.skyd.podaura.model.db.dao.FeedDao.Companion.ORDER_DELTA
import com.skyd.podaura.model.db.dao.FeedDao.Companion.ORDER_MIN_DELTA
import com.skyd.podaura.model.repository.BaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ReorderFeedRepository(
    private val feedDao: FeedDao,
    private val pagingConfig: PagingConfig,
) : BaseRepository() {
    fun requestFeedList(groupId: String?): Flow<PagingData<FeedViewBean>> = Pager(pagingConfig) {
        val realGroupId = if (groupId == GroupVo.DEFAULT_GROUP_ID) null else groupId
        feedDao.getFeedViewPagingSourceInGroup(groupId = realGroupId)
    }.flow.flowOn(Dispatchers.IO)

    fun reorderFeed(
        groupId: String?,
        fromIndex: Int,
        toIndex: Int,
    ): Flow<Int> = flow {
        if (fromIndex == toIndex) {
            emit(0)
            return@flow
        }
        val realGroupId = if (groupId == GroupVo.DEFAULT_GROUP_ID) null else groupId
        val fromFeed = feedDao.getNth(groupId = realGroupId, index = fromIndex)
        if (fromFeed == null) {
            emit(0)
            return@flow
        }
        val prevFeed = if (fromIndex < toIndex) {
            feedDao.getNth(groupId = realGroupId, index = toIndex)
        } else {
            if (toIndex - 1 >= 0) {
                feedDao.getNth(groupId = realGroupId, index = toIndex - 1)
            } else {
                null
            }
        }

        val prevOrder: Double
        val nextOrder: Double

        if (prevFeed == null) {  // Insert to first
            val minOrder = feedDao.getMinOrder(groupId = realGroupId)
            prevOrder = minOrder - ORDER_DELTA * 2
            nextOrder = minOrder
        } else {
            val nextGroup = feedDao.getNth(
                groupId = realGroupId,
                index = if (fromIndex < toIndex) toIndex + 1 else toIndex,
            )
            if (nextGroup == null) {
                val maxOrder = feedDao.getMaxOrder(groupId = realGroupId)
                prevOrder = maxOrder
                nextOrder = maxOrder + ORDER_DELTA * 2
            } else {
                prevOrder = prevFeed.orderPosition
                nextOrder = nextGroup.orderPosition
            }
        }
        if (nextOrder - prevOrder < ORDER_MIN_DELTA * 2) {
            feedDao.reindexOrders(groupId = realGroupId)
            emit(
                reorderFeed(groupId = realGroupId, fromIndex = fromIndex, toIndex = toIndex).first()
            )
            return@flow
        } else {
            emit(
                feedDao.reorderFeed(
                    url = fromFeed.url,
                    orderPosition = (prevOrder + nextOrder) / 2.0,
                )
            )
        }
    }.flowOn(Dispatchers.IO)
}
