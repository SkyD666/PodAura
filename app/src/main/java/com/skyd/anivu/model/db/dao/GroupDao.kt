package com.skyd.anivu.model.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import com.skyd.anivu.di.get
import com.skyd.anivu.model.bean.feed.FEED_TABLE_NAME
import com.skyd.anivu.model.bean.feed.FeedBean
import com.skyd.anivu.model.bean.group.GROUP_TABLE_NAME
import com.skyd.anivu.model.bean.group.GroupBean
import com.skyd.anivu.model.bean.group.GroupWithFeedBean
import com.skyd.anivu.model.bean.group.groupfeed.GroupOrFeedBean
import com.skyd.anivu.model.repository.feed.tryDeleteFeedIconFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Dao
interface GroupDao {
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
        val feedDao = get<FeedDao>()
        feedDao.getFeedsInGroup(listOf(groupId)).forEach {
            it.feed.customIcon?.let { icon -> tryDeleteFeedIconFile(icon) }
        }
        return feedDao.removeFeedByGroupId(groupId)
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
        val feedDao = get<FeedDao>()
        return feedDao.moveFeedToGroup(fromGroupId = fromGroupId, toGroupId = toGroupId)
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
    @Query(
        "SELECT EXISTS (SELECT 1 FROM `$GROUP_TABLE_NAME` " +
                "WHERE ${GroupBean.IS_EXPANDED_COLUMN} = 1)"
    )
    fun existsExpandedGroup(): Flow<Int>

    @Transaction
    @Query("SELECT COUNT(*) FROM `$GROUP_TABLE_NAME` WHERE ${GroupBean.NAME_COLUMN} LIKE :name")
    suspend fun containsByName(name: String): Int

    @Transaction
    @Query("SELECT COUNT(*) FROM `$GROUP_TABLE_NAME` WHERE ${GroupBean.GROUP_ID_COLUMN} LIKE :groupId")
    suspend fun containsById(groupId: String): Int

    @Transaction
    @Query(
        "SELECT ${GroupBean.GROUP_ID_COLUMN} FROM `$GROUP_TABLE_NAME` " +
                "WHERE ${GroupBean.NAME_COLUMN} LIKE :name " +
                "LIMIT 1"
    )
    suspend fun queryGroupIdByName(name: String): String

    @Transaction
    @Query("UPDATE `$GROUP_TABLE_NAME` SET ${GroupBean.IS_EXPANDED_COLUMN} = NOT :collapse")
    suspend fun collapseAllGroup(collapse: Boolean): Int

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        "WITH default_group_order(`order`) AS (SELECT COALESCE(MIN(`${GroupBean.ORDER_POSITION_COLUMN}`), 0) - $ORDER_DELTA FROM `$GROUP_TABLE_NAME`) " +
                "SELECT " +
                "    NULL AS ${GroupBean.GROUP_ID_COLUMN}, " +
                "    NULL AS ${FeedBean.URL_COLUMN}, " +
                "    (SELECT `order` FROM default_group_order) AS group_order, " +
                "    0 AS is_feed, " +
                "    '' AS feed_order " +
                "WHERE NOT :hideEmptyDefaultGroup OR " +
                "    EXISTS(SELECT 1 FROM `$FEED_TABLE_NAME` WHERE ${FeedBean.GROUP_ID_COLUMN} IS NULL) " +
                "UNION ALL " +
                "SELECT " +
                "    NULL AS ${GroupBean.GROUP_ID_COLUMN}, " +
                "    f.${FeedBean.URL_COLUMN} AS ${FeedBean.URL_COLUMN}, " +
                "    (SELECT `order` FROM default_group_order) AS group_order, " +
                "    1 AS is_feed, " +
                "    f.${FeedBean.TITLE_COLUMN} AS feed_order " +
                "FROM `$FEED_TABLE_NAME` f " +
                "WHERE f.${FeedBean.GROUP_ID_COLUMN} IS NULL AND " +
                "    :defaultGroupIsExpanded AND (NOT f.${FeedBean.MUTE_COLUMN} OR NOT :hideMutedFeed) " +

                "UNION ALL " +
                "SELECT * FROM (" +
                "    SELECT " +
                "        g.${GroupBean.GROUP_ID_COLUMN} AS ${GroupBean.GROUP_ID_COLUMN}, " +
                "        NULL AS ${FeedBean.URL_COLUMN}, " +
                "        g.${GroupBean.ORDER_POSITION_COLUMN} AS group_order, " +
                "        0 AS is_feed, " +
                "        '' AS feed_order " +
                "    FROM `$GROUP_TABLE_NAME` g " +
                "    UNION ALL " +
                "    SELECT " +
                "        NULL AS ${GroupBean.GROUP_ID_COLUMN}, " +
                "        f.${FeedBean.URL_COLUMN} AS ${FeedBean.URL_COLUMN}, " +
                "        g.${GroupBean.ORDER_POSITION_COLUMN} AS group_order, " +
                "        1 AS is_feed, " +
                "        f.${FeedBean.TITLE_COLUMN} AS feed_order " +
                "    FROM `$FEED_TABLE_NAME` f " +
                "    INNER JOIN `$GROUP_TABLE_NAME` g ON f.${FeedBean.GROUP_ID_COLUMN} = g.${GroupBean.GROUP_ID_COLUMN} " +
                "    WHERE (NOT f.${FeedBean.MUTE_COLUMN} OR NOT :hideMutedFeed) AND g.${GroupBean.IS_EXPANDED_COLUMN} " +
                ")" +
                "ORDER BY group_order, is_feed ASC, feed_order"
    )
    fun getGroupsAndFeeds(
        defaultGroupIsExpanded: Boolean,
        hideEmptyDefaultGroup: Boolean,
        hideMutedFeed: Boolean,
    ): PagingSource<Int, GroupOrFeedBean>

    companion object {
        const val ORDER_DELTA = 10.0
        const val ORDER_MIN_DELTA = 0.05
    }
}