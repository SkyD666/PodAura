package com.skyd.podaura.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.skyd.podaura.model.bean.article.ARTICLE_TABLE_NAME
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.history.MEDIA_PLAY_HISTORY_TABLE_NAME
import com.skyd.podaura.model.bean.history.MediaPlayHistoryBean
import com.skyd.podaura.model.bean.history.READ_HISTORY_TABLE_NAME
import com.skyd.podaura.model.bean.history.ReadHistoryBean

class Migration17To18 : Migration(17, 18) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `$MEDIA_PLAY_HISTORY_TABLE_NAME` ADD ${MediaPlayHistoryBean.ARTICLE_ID_COLUMN} TEXT")
        connection.execSQL("ALTER TABLE `$MEDIA_PLAY_HISTORY_TABLE_NAME` ADD ${MediaPlayHistoryBean.LAST_TIME_COLUMN} INTEGER NOT NULL DEFAULT 0")

        connection.execSQL(
            "CREATE TABLE `$READ_HISTORY_TABLE_NAME` (" +
                    "${ReadHistoryBean.ARTICLE_ID_COLUMN} TEXT NOT NULL PRIMARY KEY, " +
                    "${ReadHistoryBean.LAST_TIME_COLUMN} INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY (${ReadHistoryBean.ARTICLE_ID_COLUMN})" +
                    "REFERENCES $ARTICLE_TABLE_NAME(${ArticleBean.ARTICLE_ID_COLUMN})" +
                    "ON DELETE CASCADE" +
                    ")"
        )
    }
}