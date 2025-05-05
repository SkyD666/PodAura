package com.skyd.podaura.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.skyd.podaura.model.bean.article.ARTICLE_TABLE_NAME
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.article.RSS_MEDIA_TABLE_NAME
import com.skyd.podaura.model.bean.article.RssMediaBean

class Migration11To12 : Migration(11, 12) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE $ARTICLE_TABLE_NAME ADD catrgories TEXT")
        connection.execSQL(
            "CREATE TABLE `$RSS_MEDIA_TABLE_NAME` (" +
                    "${RssMediaBean.ARTICLE_ID_COLUMN} TEXT NOT NULL PRIMARY KEY, " +
                    "${RssMediaBean.DURATION_COLUMN} INTEGER, " +
                    "${RssMediaBean.ADULT_COLUMN} INTEGER NOT NULL DEFAULT 0, " +
                    "${RssMediaBean.IMAGE_COLUMN} TEXT, " +
                    "${RssMediaBean.EPISODE_COLUMN} TEXT, " +
                    "FOREIGN KEY (${RssMediaBean.ARTICLE_ID_COLUMN})" +
                    "REFERENCES $ARTICLE_TABLE_NAME(${ArticleBean.ARTICLE_ID_COLUMN})" +
                    "ON DELETE CASCADE" +
                    ")"
        )
    }
}