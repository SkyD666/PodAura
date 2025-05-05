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

class Migration14To15 : Migration(14, 15) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("DROP VIEW IF EXISTS `$FEED_VIEW_NAME`")
        connection.execSQL(
            "CREATE VIEW IF NOT EXISTS `$FEED_VIEW_NAME` AS " +
                    "SELECT $FEED_TABLE_NAME.*, IFNULL(ArticleCount.`count`, 0) AS ${FeedViewBean.ARTICLE_COUNT_COLUMN}, " +
                    "IFNULL(UnreadArticleCount.`count`, 0) AS ${FeedViewBean.UNREAD_ARTICLE_COUNT_COLUMN} " +
                    "FROM $FEED_TABLE_NAME LEFT JOIN (SELECT ${ArticleBean.FEED_URL_COLUMN}, COUNT(1) AS `count` " +
                    "FROM $ARTICLE_TABLE_NAME GROUP BY ${ArticleBean.FEED_URL_COLUMN}) AS ArticleCount " +
                    "ON $FEED_TABLE_NAME.${FeedBean.URL_COLUMN} = ArticleCount.${ArticleBean.FEED_URL_COLUMN} " +

                    "LEFT JOIN (SELECT ${ArticleBean.FEED_URL_COLUMN}, COUNT(1) AS `count` " +
                    "FROM $ARTICLE_TABLE_NAME WHERE ${ArticleBean.IS_READ_COLUMN} = 0 " +
                    "GROUP BY ${ArticleBean.FEED_URL_COLUMN}) AS UnreadArticleCount " +
                    "ON $FEED_TABLE_NAME.${FeedBean.URL_COLUMN} = UnreadArticleCount.${ArticleBean.FEED_URL_COLUMN}"
        )
    }
}