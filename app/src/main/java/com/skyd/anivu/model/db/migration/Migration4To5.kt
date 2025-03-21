package com.skyd.anivu.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.skyd.anivu.model.bean.article.ARTICLE_TABLE_NAME
import com.skyd.anivu.model.bean.article.ArticleBean
import com.skyd.anivu.model.bean.feed.FEED_TABLE_NAME
import com.skyd.anivu.model.bean.feed.FEED_VIEW_NAME
import com.skyd.anivu.model.bean.feed.FeedBean
import com.skyd.anivu.model.bean.feed.FeedViewBean

class Migration4To5 : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE VIEW IF NOT EXISTS `$FEED_VIEW_NAME` AS " +
                    "SELECT $FEED_TABLE_NAME.*, IFNULL(ArticleCount.`count`, 0) AS ${FeedViewBean.ARTICLE_COUNT_COLUMN} " +
                    "FROM $FEED_TABLE_NAME LEFT JOIN (SELECT ${ArticleBean.FEED_URL_COLUMN}, COUNT(1) AS `count` " +
                    "FROM $ARTICLE_TABLE_NAME GROUP BY ${ArticleBean.FEED_URL_COLUMN}) AS ArticleCount " +
                    "ON $FEED_TABLE_NAME.${FeedBean.URL_COLUMN} = ArticleCount.${ArticleBean.FEED_URL_COLUMN}"
        )
    }
}