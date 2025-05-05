package com.skyd.podaura.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.skyd.podaura.model.bean.history.MEDIA_PLAY_HISTORY_TABLE_NAME
import com.skyd.podaura.model.bean.history.MediaPlayHistoryBean
import com.skyd.podaura.model.bean.playlist.PLAYLIST_MEDIA_TABLE_NAME
import com.skyd.podaura.model.bean.playlist.PLAYLIST_TABLE_NAME
import com.skyd.podaura.model.bean.playlist.PLAYLIST_VIEW_NAME
import com.skyd.podaura.model.bean.playlist.PlaylistBean
import com.skyd.podaura.model.bean.playlist.PlaylistMediaBean
import com.skyd.podaura.model.bean.playlist.PlaylistViewBean

class Migration19To20 : Migration(19, 20) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `$MEDIA_PLAY_HISTORY_TABLE_NAME` ADD `${MediaPlayHistoryBean.DURATION_COLUMN}` INTEGER NOT NULL DEFAULT 0")
        connection.execSQL(
            "CREATE TABLE `$PLAYLIST_TABLE_NAME` (" +
                    "${PlaylistBean.PLAYLIST_ID_COLUMN} TEXT NOT NULL PRIMARY KEY, " +
                    "${PlaylistBean.NAME_COLUMN} TEXT NOT NULL, " +
                    "${PlaylistBean.ORDER_POSITION_COLUMN} REAL NOT NULL, " +
                    "${PlaylistBean.CREATE_TIME_COLUMN} INTEGER NOT NULL, " +
                    "${PlaylistBean.DELETE_MEDIA_ON_FINISH_COLUMN} INTEGER NOT NULL " +
                    ")"
        )
        connection.execSQL(
            "CREATE TABLE `$PLAYLIST_MEDIA_TABLE_NAME` (" +
                    "${PlaylistMediaBean.PLAYLIST_ID_COLUMN} TEXT NOT NULL, " +
                    "${PlaylistMediaBean.URL_COLUMN} TEXT NOT NULL, " +
                    "${PlaylistMediaBean.ARTICLE_ID_COLUMN} TEXT, " +
                    "${PlaylistMediaBean.ORDER_POSITION_COLUMN} REAL NOT NULL, " +
                    "${PlaylistMediaBean.CREATE_TIME_COLUMN} INTEGER NOT NULL, " +
                    "PRIMARY KEY (${PlaylistMediaBean.PLAYLIST_ID_COLUMN}, ${PlaylistMediaBean.URL_COLUMN}) " +
                    "FOREIGN KEY (${PlaylistMediaBean.PLAYLIST_ID_COLUMN}) " +
                    "REFERENCES $PLAYLIST_TABLE_NAME(${PlaylistBean.PLAYLIST_ID_COLUMN}) " +
                    "ON DELETE CASCADE" +
                    ")"
        )
        connection.execSQL(
            "CREATE VIEW `$PLAYLIST_VIEW_NAME` AS " +
                    "SELECT " +
                    "  $PLAYLIST_TABLE_NAME.*, " +
                    "  IFNULL(ItemCount.itemCount, 0) AS ${PlaylistViewBean.ITEM_COUNT_COLUMN} " +
                    "FROM " +
                    "  $PLAYLIST_TABLE_NAME " +
                    "LEFT JOIN (" +
                    "  SELECT " +
                    "    ${PlaylistMediaBean.PLAYLIST_ID_COLUMN}, " +
                    "    COUNT(1) AS itemCount " +
                    "  FROM " +
                    "    $PLAYLIST_MEDIA_TABLE_NAME " +
                    "  GROUP BY " +
                    "    ${PlaylistMediaBean.PLAYLIST_ID_COLUMN}" +
                    ") AS ItemCount " +
                    "  ON $PLAYLIST_TABLE_NAME.${PlaylistBean.PLAYLIST_ID_COLUMN} = ItemCount.${PlaylistMediaBean.PLAYLIST_ID_COLUMN}"
        )
    }
}