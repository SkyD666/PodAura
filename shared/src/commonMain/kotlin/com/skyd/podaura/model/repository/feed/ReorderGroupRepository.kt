package com.skyd.podaura.model.repository.feed

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.skyd.podaura.model.bean.group.GroupVo
import com.skyd.podaura.model.db.dao.GroupDao
import com.skyd.podaura.model.db.dao.GroupDao.Companion.ORDER_DELTA
import com.skyd.podaura.model.db.dao.GroupDao.Companion.ORDER_MIN_DELTA
import com.skyd.podaura.model.repository.BaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class ReorderGroupRepository(
    private val groupDao: GroupDao,
    private val pagingConfig: PagingConfig,
) : BaseRepository() {
    fun requestGroupList(): Flow<PagingData<GroupVo>> = Pager(pagingConfig) {
        groupDao.getGroups()
    }.flow.map { pagingData ->
        pagingData.map { it.toVo() }
    }.flowOn(Dispatchers.IO)

    fun reorderGroup(
        fromIndex: Int,
        toIndex: Int,
    ): Flow<Int> = flow {
        if (fromIndex == toIndex) {
            emit(0)
            return@flow
        }
        val fromGroup = groupDao.getNth(fromIndex)
        if (fromGroup == null) {
            emit(0)
            return@flow
        }
        val prevGroup = if (fromIndex < toIndex) {
            groupDao.getNth(toIndex)
        } else {
            if (toIndex - 1 >= 0) groupDao.getNth(toIndex - 1) else null
        }

        val prevOrder: Double
        val nextOrder: Double

        if (prevGroup == null) {  // Insert to first
            val minOrder = groupDao.getMinOrder()
            prevOrder = minOrder - ORDER_DELTA * 2
            nextOrder = minOrder
        } else {
            val nextGroup = groupDao.getNth(
                index = if (fromIndex < toIndex) toIndex + 1 else toIndex,
            )
            if (nextGroup == null) {
                val maxOrder = groupDao.getMaxOrder()
                prevOrder = maxOrder
                nextOrder = maxOrder + ORDER_DELTA * 2
            } else {
                prevOrder = prevGroup.orderPosition
                nextOrder = nextGroup.orderPosition
            }
        }
        if (nextOrder - prevOrder < ORDER_MIN_DELTA * 2) {
            groupDao.reindexOrders()
            emit(reorderGroup(fromIndex = fromIndex, toIndex = toIndex).first())
            return@flow
        } else {
            emit(
                groupDao.reorderGroup(
                    groupId = fromGroup.groupId,
                    orderPosition = (prevOrder + nextOrder) / 2.0,
                )
            )
        }
    }.flowOn(Dispatchers.IO)
}