package com.skyd.podaura.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.skyd.podaura.model.bean.article.ARTICLE_TABLE_NAME
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.feed.FEED_TABLE_NAME
import com.skyd.podaura.model.bean.feed.FeedBean

class Migration5To6 : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE $FEED_TABLE_NAME ADD ${FeedBean.CUSTOM_DESCRIPTION_COLUMN} TEXT")
        connection.execSQL("ALTER TABLE $FEED_TABLE_NAME ADD ${FeedBean.CUSTOM_ICON_COLUMN} TEXT")

        connection.execSQL("ALTER TABLE $ARTICLE_TABLE_NAME ADD ${ArticleBean.IS_READ_COLUMN} INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE $ARTICLE_TABLE_NAME ADD ${ArticleBean.IS_FAVORITE_COLUMN} INTEGER NOT NULL DEFAULT 0")
    }
}