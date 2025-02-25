package com.skyd.anivu.model.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import com.skyd.anivu.appContext
import com.skyd.anivu.model.bean.article.ARTICLE_TABLE_NAME
import com.skyd.anivu.model.bean.article.ArticleBean
import com.skyd.anivu.model.bean.article.ArticleWithEnclosureBean
import com.skyd.anivu.model.bean.article.ArticleWithFeed
import com.skyd.anivu.model.bean.article.ENCLOSURE_TABLE_NAME
import com.skyd.anivu.model.bean.article.EnclosureBean
import com.skyd.anivu.model.bean.feed.FEED_TABLE_NAME
import com.skyd.anivu.model.bean.feed.FeedBean
import com.skyd.anivu.ui.notification.ArticleNotificationManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ArticleDaoEntryPoint {
        val enclosureDao: EnclosureDao
        val rssModuleDao: RssModuleDao
    }

    // null always compares false in '='
    @Query(
        """
        SELECT * from $ARTICLE_TABLE_NAME 
        WHERE ${ArticleBean.GUID_COLUMN} = :guid AND 
        ${ArticleBean.FEED_URL_COLUMN} = :feedUrl
        """
    )
    suspend fun queryArticleByGuid(
        guid: String?,
        feedUrl: String,
    ): ArticleBean?

    // null always compares false in '='
    @Query(
        """
        SELECT * from $ARTICLE_TABLE_NAME 
        WHERE ${ArticleBean.LINK_COLUMN} = :link AND 
        ${ArticleBean.FEED_URL_COLUMN} = :feedUrl
        """
    )
    suspend fun queryArticleByLink(
        link: String?,
        feedUrl: String,
    ): ArticleBean?

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun innerUpdateArticle(articleBean: ArticleBean)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun innerUpdateArticle(articleBeanList: List<ArticleBean>)

    @Transaction
    suspend fun insertListIfNotExist(articleWithEnclosureList: List<ArticleWithEnclosureBean>) {
        val hiltEntryPoint =
            EntryPointAccessors.fromApplication(appContext, ArticleDaoEntryPoint::class.java)
        articleWithEnclosureList.forEach { articleWithEnclosure ->
            val article = articleWithEnclosure.article
            // Duplicate article by guid or link
            val guid = article.guid
            val link = article.link
            var newArticle: ArticleBean? = null
            if (guid != null) {
                newArticle = queryArticleByGuid(
                    guid = guid,
                    feedUrl = article.feedUrl,
                )
            } else if (link != null) {
                newArticle = queryArticleByLink(
                    link = link,
                    feedUrl = article.feedUrl,
                )
            }
            if (newArticle == null) {
                innerUpdateArticle(article)
                newArticle = article
            } else {
                // Update all fields except articleId
                newArticle = article.copy(articleId = newArticle.articleId)
                innerUpdateArticle(newArticle)
                articleWithEnclosure.article = newArticle
            }

            // Update modules
            val media = articleWithEnclosure.media
            if (media != null) {
                hiltEntryPoint.rssModuleDao.insertIfNotExistRssMediaBean(
                    media.copy(articleId = newArticle.articleId)
                )
            }

            hiltEntryPoint.enclosureDao.insertListIfNotExist(
                articleWithEnclosure.enclosures.map { enclosure ->
                    enclosure.copy(articleId = newArticle.articleId)
                }
            )
        }
        ArticleNotificationManager.onNewData(articleWithEnclosureList)
    }

    @Transaction
    @Delete
    suspend fun deleteArticle(articleBean: ArticleBean): Int

    @Transaction
    @Query("DELETE FROM $ARTICLE_TABLE_NAME WHERE ${ArticleBean.FEED_URL_COLUMN} LIKE :feedUrl")
    suspend fun deleteArticleInFeed(feedUrl: String): Int

    @Transaction
    @Query(
        "DELETE FROM $ARTICLE_TABLE_NAME WHERE ${ArticleBean.FEED_URL_COLUMN} IN (" +
                "SELECT ${FeedBean.URL_COLUMN} FROM $FEED_TABLE_NAME " +
                "WHERE ${FeedBean.GROUP_ID_COLUMN} IS NULL AND :groupId IS NULL OR " +
                "${FeedBean.GROUP_ID_COLUMN} = :groupId" +
                ")"
    )
    suspend fun deleteArticlesInGroup(groupId: String?): Int

    @Transaction
    @Query(
        """
        DELETE FROM $ARTICLE_TABLE_NAME
        WHERE (${ArticleBean.UPDATE_AT_COLUMN} IS NULL
        OR ${ArticleBean.UPDATE_AT_COLUMN} <= :timestamp)
        AND (:keepUnread = 0 OR ${ArticleBean.IS_READ_COLUMN} = 1)
        AND (:keepFavorite = 0 OR ${ArticleBean.IS_FAVORITE_COLUMN} = 0)
        """
    )
    suspend fun deleteArticleBefore(
        timestamp: Long,
        keepUnread: Boolean = true,
        keepFavorite: Boolean = true,
    ): Int

    @Transaction
    @Query(
        "DELETE FROM $ARTICLE_TABLE_NAME WHERE " +
                "(:keepUnread = 0 OR ${ArticleBean.IS_READ_COLUMN} = 1) AND " +
                "(:keepFavorite = 0 OR ${ArticleBean.IS_FAVORITE_COLUMN} = 0) AND " +
                "(" +
                "  ${ArticleBean.UPDATE_AT_COLUMN} IS NULL OR (" +
                "    SELECT COUNT(*) " +
                "    FROM $ARTICLE_TABLE_NAME AS a2 " +
                "    WHERE " +
                "      a2.${ArticleBean.FEED_URL_COLUMN} = $ARTICLE_TABLE_NAME.${ArticleBean.FEED_URL_COLUMN} AND " +
                "      a2.${ArticleBean.UPDATE_AT_COLUMN} > $ARTICLE_TABLE_NAME.${ArticleBean.UPDATE_AT_COLUMN}" +
                "  ) >= :count" +
                ")"
    )
    suspend fun deleteArticleExceed(
        count: Int,
        keepUnread: Boolean = true,
        keepFavorite: Boolean = true,
    ): Int

    @Transaction
    @RawQuery(observedEntities = [FeedBean::class, ArticleBean::class, EnclosureBean::class])
    fun getArticlePagingSource(sql: SupportSQLiteQuery): PagingSource<Int, ArticleWithFeed>


    @Transaction
    @RawQuery(observedEntities = [ArticleBean::class])
    fun getArticleList(sql: SupportSQLiteQuery): List<ArticleWithFeed>

    @Transaction
    @Query(
        "SELECT * FROM $ARTICLE_TABLE_NAME " +
                "WHERE ${ArticleBean.ARTICLE_ID_COLUMN} IN (:articleIds)"
    )
    fun getArticleListByIds(articleIds: List<String>): List<ArticleWithFeed>

    @Transaction
    @Query(
        """
        SELECT * FROM $ARTICLE_TABLE_NAME 
        WHERE ${ArticleBean.ARTICLE_ID_COLUMN} LIKE :articleId
        """
    )
    fun getArticleWithEnclosures(articleId: String): Flow<ArticleWithEnclosureBean?>

    @Transaction
    @Query(
        "SELECT COUNT(1) FROM $ARTICLE_TABLE_NAME " +
                "WHERE ${ArticleBean.ARTICLE_ID_COLUMN} LIKE :articleId"
    )
    fun exists(articleId: String): Int

    @Transaction
    @Query(
        "WITH temp_target(feed_url, update_time) AS (SELECT ${ArticleBean.FEED_URL_COLUMN}, ${ArticleBean.DATE_COLUMN} FROM $ARTICLE_TABLE_NAME WHERE ${ArticleBean.ARTICLE_ID_COLUMN} = :articleId), " +
                "temp_enclosure(enclosure_count) AS (SELECT COUNT(1) FROM $ENCLOSURE_TABLE_NAME WHERE ${EnclosureBean.ARTICLE_ID_COLUMN} = $ARTICLE_TABLE_NAME.${ArticleBean.ARTICLE_ID_COLUMN}) " +
                "SELECT * FROM ( " +
                "    SELECT * FROM $ARTICLE_TABLE_NAME " +
                "    WHERE ${ArticleBean.FEED_URL_COLUMN} = (SELECT feed_url FROM temp_target) AND " +
                "       (SELECT enclosure_count FROM temp_enclosure) > 0 AND " +
                "       (${ArticleBean.DATE_COLUMN} > (SELECT update_time FROM temp_target) " +
                "           OR (${ArticleBean.DATE_COLUMN} = (SELECT update_time FROM temp_target) AND ${ArticleBean.ARTICLE_ID_COLUMN} > :articleId)) " +
                "    ORDER BY ${ArticleBean.DATE_COLUMN} DESC, ${ArticleBean.ARTICLE_ID_COLUMN} DESC " +
                "    LIMIT :neighborCount " +
                ") " +
                "UNION ALL " +
                "SELECT * FROM $ARTICLE_TABLE_NAME WHERE ${ArticleBean.ARTICLE_ID_COLUMN} = :articleId " +
                "UNION ALL " +
                "SELECT * FROM ( " +
                "    SELECT * FROM $ARTICLE_TABLE_NAME " +
                "    WHERE ${ArticleBean.FEED_URL_COLUMN} = (SELECT feed_url FROM temp_target) AND " +
                "       (SELECT enclosure_count FROM temp_enclosure) > 0 AND " +
                "       (${ArticleBean.DATE_COLUMN} < (SELECT update_time FROM temp_target) " +
                "           OR (${ArticleBean.DATE_COLUMN} = (SELECT update_time FROM temp_target) AND ${ArticleBean.ARTICLE_ID_COLUMN} < :articleId)) " +
                "    ORDER BY ${ArticleBean.DATE_COLUMN} DESC, ${ArticleBean.ARTICLE_ID_COLUMN} DESC " +
                "    LIMIT :neighborCount " +
                ");"
    )
    fun getArticlesForPlaylist(articleId: String, neighborCount: Int = 50): List<ArticleWithFeed>

    @Transaction
    @Query(
        """
        SELECT * FROM $ARTICLE_TABLE_NAME 
        WHERE ${ArticleBean.ARTICLE_ID_COLUMN} LIKE :articleId
        """
    )
    fun getArticleWithFeed(articleId: String): Flow<ArticleWithFeed?>

    @RewriteQueriesToDropUnusedColumns
    @Query(
        """
        SELECT a.*
        FROM $ARTICLE_TABLE_NAME AS a LEFT JOIN $FEED_TABLE_NAME AS f 
        ON a.${ArticleBean.FEED_URL_COLUMN} = f.${FeedBean.URL_COLUMN}
        WHERE a.${ArticleBean.FEED_URL_COLUMN} = :feedUrl 
        ORDER BY date DESC LIMIT 1
        """
    )
    suspend fun queryLatestByFeedUrl(feedUrl: String): ArticleBean?

    @Transaction
    @Query(
        """
        UPDATE $ARTICLE_TABLE_NAME SET ${ArticleBean.IS_FAVORITE_COLUMN} = :favorite
        WHERE ${ArticleBean.ARTICLE_ID_COLUMN} = :articleId
        """
    )
    fun favoriteArticle(articleId: String, favorite: Boolean)

    @Transaction
    @Query(
        """
        UPDATE $ARTICLE_TABLE_NAME SET ${ArticleBean.IS_READ_COLUMN} = :read
        WHERE ${ArticleBean.ARTICLE_ID_COLUMN} = :articleId
        """
    )
    fun readArticle(articleId: String, read: Boolean)

    @Transaction
    @Query(
        """
        UPDATE $ARTICLE_TABLE_NAME SET ${ArticleBean.IS_READ_COLUMN} = 1
        WHERE ${ArticleBean.IS_READ_COLUMN} = 0 AND ${ArticleBean.FEED_URL_COLUMN} = :feedUrl
        """
    )
    fun readAllInFeed(feedUrl: String): Int

    @Transaction
    @Query(
        """
        UPDATE $ARTICLE_TABLE_NAME SET ${ArticleBean.IS_READ_COLUMN} = 1
        WHERE ${ArticleBean.IS_READ_COLUMN} = 0 AND ${ArticleBean.FEED_URL_COLUMN} IN (
            SELECT DISTINCT ${FeedBean.URL_COLUMN} FROM $FEED_TABLE_NAME
            WHERE ${FeedBean.GROUP_ID_COLUMN} = :groupId OR
            :groupId IS NULL AND ${FeedBean.GROUP_ID_COLUMN} IS NULL
        )
        """
    )
    fun readAllInGroup(groupId: String?): Int
}