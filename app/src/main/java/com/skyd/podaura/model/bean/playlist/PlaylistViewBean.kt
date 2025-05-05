package com.skyd.podaura.model.bean.playlist

import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.Embedded
import androidx.room.Ignore

const val PLAYLIST_VIEW_NAME = "PlaylistView"

@DatabaseView(
    value = "SELECT " +
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
            "  ON $PLAYLIST_TABLE_NAME.${PlaylistBean.PLAYLIST_ID_COLUMN} = ItemCount.${PlaylistMediaBean.PLAYLIST_ID_COLUMN}",
    viewName = PLAYLIST_VIEW_NAME
)
data class PlaylistViewBean(
    @Embedded
    val playlist: PlaylistBean,
    @ColumnInfo(name = ITEM_COUNT_COLUMN)
    val itemCount: Int = 0,
) {
    @Ignore
    var thumbnails: List<String> = listOf()

    companion object {
        const val ITEM_COUNT_COLUMN = "itemCount"
    }
}