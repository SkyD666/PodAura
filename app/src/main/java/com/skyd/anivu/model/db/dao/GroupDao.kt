package com.skyd.anivu.model.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.skyd.anivu.appContext
import com.skyd.anivu.model.bean.group.GROUP_TABLE_NAME
import com.skyd.anivu.model.bean.group.GroupBean
import com.skyd.anivu.model.bean.group.GroupWithFeedBean
import com.skyd.anivu.model.repository.feed.tryDeleteFeedIconFile
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Dao
interface GroupDao {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface GroupDaoEntryPoint {
        val feedDao: FeedDao
    }

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setGroup(groupBean: GroupBean)

    @Transaction
    @Query("SELECT * FROM `$GROUP_TABLE_NAME` WHERE ${GroupBean.GROUP_ID_COLUMN} = :groupId")
    suspend fun getGroupById(groupId: String): GroupBean

    @Transaction
    @Query("DELETE FROM `$GROUP_TABLE_NAME` WHERE ${GroupBean.GROUP_ID_COLUMN} = :groupId")
    suspend fun innerRemoveGroup(groupId: String): Int

    @Transaction
    @Query(
        "UPDATE `$GROUP_TABLE_NAME` SET ${GroupBean.NAME_COLUMN} = :name " +
                "WHERE ${GroupBean.GROUP_ID_COLUMN} = :groupId"
    )
    suspend fun renameGroup(groupId: String, name: String): Int


    @Transaction
    suspend fun removeGroupWithFeed(groupId: String): Int {
        innerRemoveGroup(groupId)
        return EntryPointAccessors.fromApplication(appContext, GroupDaoEntryPoint::class.java).run {
            feedDao.getFeedsInGroup(listOf(groupId)).forEach {
                it.feed.customIcon?.let { icon -> tryDeleteFeedIconFile(icon) }
            }
            feedDao.removeFeedByGroupId(groupId)
        }
    }

    @Transaction
    @Query(
        "SELECT * FROM `$GROUP_TABLE_NAME` " +
                "ORDER BY ${GroupBean.ORDER_POSITION_COLUMN} " +
                "LIMIT 1 OFFSET :index"
    )
    suspend fun getNth(index: Int): GroupBean?

    @Transaction
    @Query("SELECT COALESCE(MAX(`${GroupBean.ORDER_POSITION_COLUMN}`), 0) FROM `$GROUP_TABLE_NAME`")
    suspend fun getMaxOrder(): Double

    @Transaction
    @Query("SELECT COALESCE(MIN(`${GroupBean.ORDER_POSITION_COLUMN}`), 0) FROM `$GROUP_TABLE_NAME`")
    suspend fun getMinOrder(): Double

    @Transaction
    @Query(
        "UPDATE `$GROUP_TABLE_NAME` " +
                "SET ${GroupBean.ORDER_POSITION_COLUMN} = :orderPosition " +
                "WHERE ${GroupBean.GROUP_ID_COLUMN} = :groupId"
    )
    suspend fun reorderGroup(groupId: String, orderPosition: Double): Int

    @Transaction
    suspend fun reindexOrders() {
        getGroupIds().first().forEachIndexed { index, item ->
            reorderGroup(
                groupId = item,
                orderPosition = (index * ORDER_DELTA) + ORDER_DELTA,
            )
        }
    }

    @Transaction
    suspend fun moveGroupFeedsTo(fromGroupId: String?, toGroupId: String?): Int {
        return EntryPointAccessors.fromApplication(appContext, GroupDaoEntryPoint::class.java).run {
            feedDao.moveFeedToGroup(fromGroupId = fromGroupId, toGroupId = toGroupId)
        }
    }

    @Transaction
    @Query(
        "UPDATE `$GROUP_TABLE_NAME` SET ${GroupBean.IS_EXPANDED_COLUMN} = :expanded " +
                "WHERE ${GroupBean.GROUP_ID_COLUMN} = :groupId"
    )
    suspend fun changeGroupExpanded(groupId: String, expanded: Boolean): Int

    @Transaction
    @Query("SELECT * FROM `$GROUP_TABLE_NAME` ORDER BY ${GroupBean.ORDER_POSITION_COLUMN}")
    fun getGroupWithFeeds(): Flow<List<GroupWithFeedBean>>

    @Transaction
    @Query("SELECT * FROM `$GROUP_TABLE_NAME` ORDER BY ${GroupBean.ORDER_POSITION_COLUMN}")
    fun getGroups(): PagingSource<Int, GroupBean>

    @Transaction
    @Query(
        "SELECT DISTINCT ${GroupBean.GROUP_ID_COLUMN} FROM `$GROUP_TABLE_NAME` " +
                "ORDER BY ${GroupBean.ORDER_POSITION_COLUMN}"
    )
    fun getGroupIds(): Flow<List<String>>

    @Transaction
    @Query("SELECT COUNT(*) FROM `$GROUP_TABLE_NAME` WHERE ${GroupBean.NAME_COLUMN} LIKE :name")
    fun containsByName(name: String): Int

    @Transaction
    @Query("SELECT COUNT(*) FROM `$GROUP_TABLE_NAME` WHERE ${GroupBean.GROUP_ID_COLUMN} LIKE :groupId")
    fun containsById(groupId: String): Int

    @Transaction
    @Query(
        "SELECT ${GroupBean.GROUP_ID_COLUMN} FROM `$GROUP_TABLE_NAME` " +
                "WHERE ${GroupBean.NAME_COLUMN} LIKE :name " +
                "LIMIT 1"
    )
    fun queryGroupIdByName(name: String): String

    companion object {
        const val ORDER_DELTA = 10.0
        const val ORDER_MIN_DELTA = 0.05
    }
}