package com.skyd.podaura.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.skyd.podaura.model.bean.article.ARTICLE_TABLE_NAME
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.feed.FEED_TABLE_NAME
import com.skyd.podaura.model.bean.feed.FEED_VIEW_NAME
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.bean.feed.FeedViewBean

class Migration15To16 : Migration(15, 16) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("DROP VIEW IF EXISTS `$FEED_VIEW_NAME`")
        connection.execSQL(
            "CREATE VIEW `$FEED_VIEW_NAME` AS " +
                    "SELECT " +
                    "  $FEED_TABLE_NAME.*, " +
                    "  IFNULL(ArticleCount.totalCount, 0) AS ${FeedViewBean.ARTICLE_COUNT_COLUMN}, " +
                    "  IFNULL(ArticleCount.unreadCount, 0) AS ${FeedViewBean.UNREAD_ARTICLE_COUNT_COLUMN} " +
                    "FROM " +
                    "  $FEED_TABLE_NAME " +
                    "LEFT JOIN (" +
                    "  SELECT " +
                    "    ${ArticleBean.FEED_URL_COLUMN}, " +
                    "    COUNT(1) AS totalCount, " +
                    "    COUNT(CASE WHEN ${ArticleBean.IS_READ_COLUMN} = 0 THEN 1 END) AS unreadCount " +
                    "  FROM " +
                    "    $ARTICLE_TABLE_NAME " +
                    "  GROUP BY " +
                    "    ${ArticleBean.FEED_URL_COLUMN}" +
                    ") AS ArticleCount " +
                    "  ON $FEED_TABLE_NAME.${FeedBean.URL_COLUMN} = ArticleCount.${ArticleBean.FEED_URL_COLUMN}"
        )
    }
}