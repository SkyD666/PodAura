package com.skyd.podaura.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.skyd.podaura.model.bean.article.ARTICLE_TABLE_NAME
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.history.MEDIA_PLAY_HISTORY_TABLE_NAME
import com.skyd.podaura.model.bean.history.MediaPlayHistoryBean
import com.skyd.podaura.model.bean.playlist.PLAYLIST_MEDIA_TABLE_NAME
import com.skyd.podaura.model.bean.playlist.PLAYLIST_TABLE_NAME
import com.skyd.podaura.model.bean.playlist.PlaylistBean
import com.skyd.podaura.model.bean.playlist.PlaylistMediaBean

class Migration22To23 : Migration(22, 23) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE `${PLAYLIST_MEDIA_TABLE_NAME}_Backup` (" +
                    "${PlaylistMediaBean.PLAYLIST_ID_COLUMN} TEXT NOT NULL, " +
                    "${PlaylistMediaBean.URL_COLUMN} TEXT NOT NULL, " +
                    "${PlaylistMediaBean.ARTICLE_ID_COLUMN} TEXT, " +
                    "${PlaylistMediaBean.ORDER_POSITION_COLUMN} REAL NOT NULL, " +
                    "${PlaylistMediaBean.CREATE_TIME_COLUMN} INTEGER NOT NULL, " +
                    "PRIMARY KEY (${PlaylistMediaBean.PLAYLIST_ID_COLUMN}, ${PlaylistMediaBean.URL_COLUMN}) " +
                    "FOREIGN KEY (${PlaylistMediaBean.PLAYLIST_ID_COLUMN}) " +
                    "    REFERENCES $PLAYLIST_TABLE_NAME(${PlaylistBean.PLAYLIST_ID_COLUMN}) " +
                    "    ON DELETE CASCADE, " +
                    "FOREIGN KEY (${PlaylistMediaBean.ARTICLE_ID_COLUMN}) " +
                    "    REFERENCES $ARTICLE_TABLE_NAME(${ArticleBean.ARTICLE_ID_COLUMN}) " +
                    "    ON DELETE CASCADE" +
                    ")"
        )
        connection.execSQL(
            "CREATE TABLE `${MEDIA_PLAY_HISTORY_TABLE_NAME}_Backup` (" +
                    "${MediaPlayHistoryBean.PATH_COLUMN} TEXT NOT NULL PRIMARY KEY, " +
                    "${MediaPlayHistoryBean.DURATION_COLUMN} INTEGER NOT NULL, " +
                    "${MediaPlayHistoryBean.LAST_PLAY_POSITION_COLUMN} INTEGER NOT NULL, " +
                    "${MediaPlayHistoryBean.LAST_TIME_COLUMN} INTEGER NOT NULL, " +
                    "${MediaPlayHistoryBean.ARTICLE_ID_COLUMN} TEXT, " +
                    "FOREIGN KEY (${MediaPlayHistoryBean.ARTICLE_ID_COLUMN}) " +
                    "    REFERENCES $ARTICLE_TABLE_NAME(${ArticleBean.ARTICLE_ID_COLUMN}) " +
                    "    ON DELETE CASCADE" +
                    ")"
        )
        connection.execSQL("INSERT OR IGNORE INTO ${PLAYLIST_MEDIA_TABLE_NAME}_Backup SELECT * FROM $PLAYLIST_MEDIA_TABLE_NAME")
        connection.execSQL("INSERT OR IGNORE INTO ${MEDIA_PLAY_HISTORY_TABLE_NAME}_Backup SELECT * FROM $MEDIA_PLAY_HISTORY_TABLE_NAME")
        connection.execSQL("DROP TABLE `$PLAYLIST_MEDIA_TABLE_NAME`")
        connection.execSQL("ALTER TABLE ${PLAYLIST_MEDIA_TABLE_NAME}_Backup RENAME to `$PLAYLIST_MEDIA_TABLE_NAME`")
        connection.execSQL("DROP TABLE `$MEDIA_PLAY_HISTORY_TABLE_NAME`")
        connection.execSQL("ALTER TABLE ${MEDIA_PLAY_HISTORY_TABLE_NAME}_Backup RENAME to `$MEDIA_PLAY_HISTORY_TABLE_NAME`")
    }
}