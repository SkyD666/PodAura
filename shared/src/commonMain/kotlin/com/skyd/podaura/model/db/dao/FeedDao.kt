package com.skyd.podaura.model.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import androidx.room.Transaction
import androidx.room.Update
import com.skyd.fundation.di.get
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.feed.FEED_TABLE_NAME
import com.skyd.podaura.model.bean.feed.FEED_VIEW_NAME
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.bean.feed.FeedViewBean
import com.skyd.podaura.model.bean.feed.FeedWithArticleBean
import com.skyd.podaura.model.bean.group.GROUP_TABLE_NAME
import com.skyd.podaura.model.bean.group.GroupBean
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDao {
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setFeed(feedBean: FeedBean)

    @Transaction
    @Update
    suspend fun updateFeed(feedBean: FeedBean)

    @Transaction
    suspend fun setFeedWithArticle(feedWithArticleBean: FeedWithArticleBean) {
        if (containsByUrl(feedWithArticleBean.feed.url) == 0) {
            setFeed(feedWithArticleBean.feed)
        } else {
            updateFeed(feedWithArticleBean.feed)
        }
        val feedUrl = feedWithArticleBean.feed.url
        get<ArticleDao>().insertListIfNotExist(
            feedWithArticleBean.articles.map { articleWithEnclosure ->
                val articleId = articleWithEnclosure.article.articleId

                // Add ArticleWithEnclosure
                return@map if (articleWithEnclosure.article.feedUrl != feedUrl) {
                    articleWithEnclosure.copy(
                        article = articleWithEnclosure.article.copy(feedUrl = feedUrl),
                        enclosures = articleWithEnclosure.enclosures.map {
                            if (it.articleId != articleId) it.copy(articleId = articleId)
                            else it
                        }
                    )
                } else articleWithEnclosure
            }
        )
    }

    @Transaction
    @Delete
    suspend fun removeFeed(feedBean: FeedBean): Int

    @Transaction
    @Query("DELETE FROM $FEED_TABLE_NAME WHERE ${FeedBean.URL_COLUMN} = :url")
    suspend fun removeFeed(url: String): Int

    @Transaction
    @Query("DELETE FROM $FEED_TABLE_NAME WHERE ${FeedBean.GROUP_ID_COLUMN} = :groupId")
    suspend fun removeFeedByGroupId(groupId: String): Int

    @Transaction
    @Query(
        """
        UPDATE $FEED_TABLE_NAME
        SET ${FeedBean.GROUP_ID_COLUMN} = :toGroupId, 
            ${FeedBean.ORDER_POSITION_COLUMN} = ${FeedBean.ORDER_POSITION_COLUMN} - :fromGroupFeedMinOrder + :toGroupFeedMaxOrder + :orderDelta 
        WHERE :fromGroupId IS NULL AND ${FeedBean.GROUP_ID_COLUMN} IS NULL OR
        ${FeedBean.GROUP_ID_COLUMN} = :fromGroupId OR
        :fromGroupId IS NULL AND ${FeedBean.GROUP_ID_COLUMN} NOT IN (
            SELECT DISTINCT ${GroupBean.GROUP_ID_COLUMN} FROM `$GROUP_TABLE_NAME`
        )
        """
    )
    suspend fun moveFeedToGroup(
        fromGroupId: String?,
        toGroupId: String?,
        fromGroupFeedMinOrder: Double,
        toGroupFeedMaxOrder: Double,
        orderDelta: Double,
    ): Int

    @Transaction
    @Query(
        """
        UPDATE $FEED_TABLE_NAME
        SET ${FeedBean.ICON_COLUMN} = :icon
        WHERE ${FeedBean.URL_COLUMN} = :feedUrl
        """
    )
    suspend fun updateFeedIcon(feedUrl: String, icon: String?): Int

    @Transaction
    @Query(
        """
        UPDATE $FEED_TABLE_NAME
        SET ${FeedBean.REQUEST_HEADERS_COLUMN} = :headers
        WHERE ${FeedBean.URL_COLUMN} = :feedUrl
        """
    )
    suspend fun updateFeedHeaders(feedUrl: String, headers: FeedBean.RequestHeaders?)

    @Transaction
    @Query(
        """
        SELECT ${FeedBean.REQUEST_HEADERS_COLUMN} FROM $FEED_TABLE_NAME
        WHERE ${FeedBean.URL_COLUMN} = :feedUrl
        """
    )
    fun getFeedHeaders(feedUrl: String): Flow<FeedBean.RequestHeaders?>

    @Transaction
    @Query(
        """
        UPDATE $FEED_TABLE_NAME
        SET ${FeedBean.SORT_XML_ARTICLES_ON_UPDATE_COLUMN} = :sort
        WHERE ${FeedBean.URL_COLUMN} = :feedUrl
        """
    )
    suspend fun updateFeedSortXmlArticlesOnUpdate(feedUrl: String, sort: Boolean): Int

    @Transaction
    @Query(
        "SELECT * FROM $FEED_VIEW_NAME " +
                "WHERE :groupId IS NULL AND ${FeedBean.GROUP_ID_COLUMN} IS NULL " +
                "OR ${FeedBean.GROUP_ID_COLUMN} = :groupId " +
                "ORDER BY ${FeedBean.ORDER_POSITION_COLUMN}"
    )
    fun getFeedViewPagingSourceInGroup(groupId: String?): PagingSource<Int, FeedViewBean>

    @Transaction
    @Query("SELECT * FROM $FEED_VIEW_NAME WHERE ${FeedBean.URL_COLUMN} = :feedUrl")
    suspend fun getFeedView(feedUrl: String): FeedViewBean

    @Transaction
    @Query("SELECT * FROM $FEED_TABLE_NAME WHERE ${FeedBean.URL_COLUMN} = :feedUrl")
    suspend fun getFeed(feedUrl: String): FeedBean?

    @Transaction
    @Query("SELECT * FROM $FEED_VIEW_NAME WHERE ${FeedBean.URL_COLUMN} IN (:feedUrls)")
    suspend fun getFeedsIn(feedUrls: List<String>): List<FeedViewBean>

    @Transaction
    @Query("SELECT * FROM $FEED_VIEW_NAME WHERE ${FeedBean.GROUP_ID_COLUMN} IN (:groupIds)")
    suspend fun getFeedsInGroup(groupIds: List<String>): List<FeedViewBean>

    @Transaction
    @Query("SELECT ${FeedBean.URL_COLUMN} FROM $FEED_TABLE_NAME WHERE ${FeedBean.GROUP_ID_COLUMN} IN (:groupIds)")
    suspend fun getFeedUrlsInGroup(groupIds: List<String>): List<String>

    @Transaction
    @Query("SELECT ${FeedBean.URL_COLUMN} FROM $FEED_TABLE_NAME WHERE ${FeedBean.GROUP_ID_COLUMN} IS NULL")
    suspend fun getFeedUrlsInDefaultGroup(): List<String>

    @Transaction
    @Query(
        "SELECT * FROM $FEED_VIEW_NAME " +
                "WHERE ${FeedBean.GROUP_ID_COLUMN} IS NULL OR " +
                "${FeedBean.GROUP_ID_COLUMN} NOT IN (:groupIds) " +
                "ORDER BY ${FeedBean.ORDER_POSITION_COLUMN}"
    )
    suspend fun getFeedsNotInGroup(groupIds: List<String>): List<FeedViewBean>

    @Transaction
    @Query("SELECT * FROM $FEED_VIEW_NAME WHERE ${FeedBean.GROUP_ID_COLUMN} IS NULL")
    fun getFeedsInDefaultGroup(): Flow<List<FeedViewBean>>

    @Transaction
    @Query(
        """
            SELECT * FROM $FEED_VIEW_NAME
            WHERE :groupId IS NULL AND ${FeedBean.GROUP_ID_COLUMN} IS NULL OR
            ${FeedBean.GROUP_ID_COLUMN} = :groupId
        """
    )
    suspend fun getFeedsByGroupId(groupId: String?): List<FeedViewBean>

    @Transaction
    @Query(
        "SELECT ${FeedBean.URL_COLUMN} FROM $FEED_TABLE_NAME " +
                "WHERE :groupId IS NULL AND ${FeedBean.GROUP_ID_COLUMN} IS NULL " +
                "OR ${FeedBean.GROUP_ID_COLUMN} = :groupId"
    )
    suspend fun getFeedUrlsByGroupId(groupId: String?): List<String>

    @Transaction
    @RawQuery(observedEntities = [FeedBean::class, ArticleBean::class])
    fun getFeedPagingSource(sql: RoomRawQuery): PagingSource<Int, FeedViewBean>

    @Transaction
    @RawQuery(observedEntities = [FeedBean::class, ArticleBean::class])
    suspend fun getFeedList(sql: RoomRawQuery): List<FeedViewBean>

    @Transaction
    @Query("SELECT * FROM $FEED_TABLE_NAME")
    fun getAllFeedList(): Flow<List<FeedBean>>

    @Transaction
    @Query("SELECT ${FeedBean.URL_COLUMN} FROM $FEED_TABLE_NAME")
    suspend fun getAllFeedUrl(): List<String>

    @Transaction
    @Query("SELECT ${FeedBean.URL_COLUMN} FROM $FEED_TABLE_NAME WHERE ${FeedBean.MUTE_COLUMN} = 0")
    suspend fun getAllUnmutedFeedUrl(): List<String>

    @Transaction
    @Query("SELECT COUNT(*) FROM $FEED_TABLE_NAME WHERE ${FeedBean.URL_COLUMN} LIKE :url")
    suspend fun containsByUrl(url: String): Int

    @Transaction
    @Query("SELECT COUNT(*) FROM $FEED_TABLE_NAME WHERE ${FeedBean.CUSTOM_ICON_COLUMN} LIKE :customIcon")
    suspend fun containsByCustomIcon(customIcon: String): Int

    @Transaction
    @Query(
        "UPDATE $FEED_TABLE_NAME SET ${FeedBean.MUTE_COLUMN} = :mute " +
                "WHERE ${FeedBean.URL_COLUMN} = :feedUrl"
    )
    suspend fun muteFeed(feedUrl: String, mute: Boolean): Int

    @Transaction
    @Query(
        "UPDATE $FEED_TABLE_NAME SET ${FeedBean.MUTE_COLUMN} = :mute " +
                "WHERE ${FeedBean.GROUP_ID_COLUMN} IS NULL AND :groupId IS NULL " +
                "OR ${FeedBean.GROUP_ID_COLUMN} = :groupId"
    )
    suspend fun muteFeedsInGroup(groupId: String?, mute: Boolean): Int

    @Transaction
    @Query(
        "UPDATE `$FEED_TABLE_NAME` " +
                "SET ${FeedBean.FILTER_MASK_COLUMN} = :filterMask " +
                "WHERE ${FeedBean.URL_COLUMN} = :url"
    )
    suspend fun updateFilterMask(url: String, filterMask: Int): Int

    @Transaction
    @Query(
        "SELECT ${FeedBean.FILTER_MASK_COLUMN} FROM `$FEED_TABLE_NAME` " +
                "WHERE ${FeedBean.URL_COLUMN} = :url"
    )
    suspend fun getFilterMask(url: String): Int

    @Transaction
    @Query(
        "UPDATE `$FEED_TABLE_NAME` " +
                "SET ${FeedBean.ORDER_POSITION_COLUMN} = :orderPosition " +
                "WHERE ${FeedBean.URL_COLUMN} = :url"
    )
    suspend fun reorderFeed(url: String, orderPosition: Double): Int

    @Transaction
    suspend fun reindexOrders(groupId: String?) {
        getFeedUrlsByGroupId(groupId).forEachIndexed { index, url ->
            reorderFeed(
                url = url,
                orderPosition = (index * ORDER_DELTA) + ORDER_DELTA,
            )
        }
    }

    @Transaction
    @Query(
        "SELECT * FROM `$FEED_TABLE_NAME` " +
                "WHERE ${FeedBean.GROUP_ID_COLUMN} IS NULL AND :groupId IS NULL " +
                "OR ${FeedBean.GROUP_ID_COLUMN} = :groupId " +
                "ORDER BY ${FeedBean.ORDER_POSITION_COLUMN} " +
                "LIMIT 1 OFFSET :index"
    )
    suspend fun getNth(groupId: String?, index: Int): FeedBean?

    @Transaction
    @Query(
        "SELECT COALESCE(MAX(`${FeedBean.ORDER_POSITION_COLUMN}`), 0) " +
                "FROM `$FEED_TABLE_NAME` " +
                "WHERE ${FeedBean.GROUP_ID_COLUMN} IS NULL AND :groupId IS NULL " +
                "OR ${FeedBean.GROUP_ID_COLUMN} = :groupId"
    )
    suspend fun getMaxOrder(groupId: String?): Double

    @Transaction
    @Query(
        "SELECT COALESCE(MIN(`${FeedBean.ORDER_POSITION_COLUMN}`), 0) " +
                "FROM `$FEED_TABLE_NAME` " +
                "WHERE ${FeedBean.GROUP_ID_COLUMN} IS NULL AND :groupId IS NULL " +
                "OR ${FeedBean.GROUP_ID_COLUMN} = :groupId"
    )
    suspend fun getMinOrder(groupId: String?): Double

    companion object {
        const val ORDER_DELTA = 10.0
        const val ORDER_MIN_DELTA = 1E-5
    }
}