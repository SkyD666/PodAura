package com.skyd.anivu.model.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.skyd.anivu.appContext
import com.skyd.anivu.model.bean.article.ArticleBean
import com.skyd.anivu.model.bean.feed.FEED_TABLE_NAME
import com.skyd.anivu.model.bean.feed.FEED_VIEW_NAME
import com.skyd.anivu.model.bean.feed.FeedBean
import com.skyd.anivu.model.bean.feed.FeedViewBean
import com.skyd.anivu.model.bean.feed.FeedWithArticleBean
import com.skyd.anivu.model.bean.group.GROUP_TABLE_NAME
import com.skyd.anivu.model.bean.group.GroupBean
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDao {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface FeedDaoEntryPoint {
        val articleDao: ArticleDao
    }

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
        val hiltEntryPoint =
            EntryPointAccessors.fromApplication(appContext, FeedDaoEntryPoint::class.java)
        val feedUrl = feedWithArticleBean.feed.url
        hiltEntryPoint.articleDao.insertListIfNotExist(
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
        SET ${FeedBean.GROUP_ID_COLUMN} = :toGroupId
        WHERE :fromGroupId IS NULL AND ${FeedBean.GROUP_ID_COLUMN} IS NULL OR
        ${FeedBean.GROUP_ID_COLUMN} = :fromGroupId OR
        :fromGroupId IS NULL AND ${FeedBean.GROUP_ID_COLUMN} NOT IN (
            SELECT DISTINCT ${GroupBean.GROUP_ID_COLUMN} FROM `$GROUP_TABLE_NAME`
        )
        """
    )
    suspend fun moveFeedToGroup(fromGroupId: String?, toGroupId: String?): Int

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
    @Query("SELECT * FROM $FEED_TABLE_NAME")
    fun getFeedPagingSource(): PagingSource<Int, FeedBean>

    @Transaction
    @Query("SELECT * FROM $FEED_VIEW_NAME WHERE ${FeedBean.URL_COLUMN} = :feedUrl")
    suspend fun getFeed(feedUrl: String): FeedViewBean

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
        """
            SELECT * FROM $FEED_VIEW_NAME
            WHERE ${FeedBean.GROUP_ID_COLUMN} IS NULL OR 
            ${FeedBean.GROUP_ID_COLUMN} NOT IN (:groupIds)
        """
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
    @RawQuery(observedEntities = [FeedBean::class, ArticleBean::class])
    fun getFeedPagingSource(sql: SupportSQLiteQuery): PagingSource<Int, FeedViewBean>

    @Transaction
    @RawQuery(observedEntities = [FeedBean::class, ArticleBean::class])
    fun getFeedList(sql: SupportSQLiteQuery): List<FeedViewBean>

    @Transaction
    @Query("SELECT * FROM $FEED_TABLE_NAME")
    fun getAllFeedList(): Flow<List<FeedBean>>

    @Transaction
    @Query("SELECT ${FeedBean.URL_COLUMN} FROM $FEED_TABLE_NAME")
    fun getAllFeedUrl(): List<String>

    @Transaction
    @Query("SELECT ${FeedBean.URL_COLUMN} FROM $FEED_TABLE_NAME WHERE ${FeedBean.MUTE_COLUMN} = 0")
    fun getAllUnmutedFeedUrl(): List<String>

    @Transaction
    @Query("SELECT COUNT(*) FROM $FEED_TABLE_NAME WHERE ${FeedBean.URL_COLUMN} LIKE :url")
    fun containsByUrl(url: String): Int

    @Transaction
    @Query("SELECT COUNT(*) FROM $FEED_TABLE_NAME WHERE ${FeedBean.CUSTOM_ICON_COLUMN} LIKE :customIcon")
    fun containsByCustomIcon(customIcon: String): Int

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
}