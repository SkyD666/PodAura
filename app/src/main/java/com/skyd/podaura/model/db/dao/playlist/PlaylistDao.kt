package com.skyd.podaura.model.db.dao.playlist

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import androidx.room.Transaction
import com.skyd.podaura.model.bean.playlist.PLAYLIST_TABLE_NAME
import com.skyd.podaura.model.bean.playlist.PLAYLIST_VIEW_NAME
import com.skyd.podaura.model.bean.playlist.PlaylistBean
import com.skyd.podaura.model.bean.playlist.PlaylistViewBean

@Dao
interface PlaylistDao {
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPlaylist(playlistBean: PlaylistBean)

    @Transaction
    @Query("DELETE FROM $PLAYLIST_TABLE_NAME WHERE ${PlaylistBean.PLAYLIST_ID_COLUMN} = :playlistId")
    suspend fun deletePlaylist(playlistId: String): Int

    @Transaction
    @Query(
        "UPDATE `$PLAYLIST_TABLE_NAME` " +
                "SET ${PlaylistBean.ORDER_POSITION_COLUMN} = :orderPosition " +
                "WHERE ${PlaylistBean.PLAYLIST_ID_COLUMN} = :playlistId"
    )
    suspend fun reorderPlaylist(playlistId: String, orderPosition: Double): Int

    @Transaction
    @Query(
        "UPDATE `$PLAYLIST_TABLE_NAME` SET ${PlaylistBean.NAME_COLUMN} = :name " +
                "WHERE ${PlaylistBean.PLAYLIST_ID_COLUMN} = :playlistId"
    )
    suspend fun renamePlaylist(playlistId: String, name: String): Int

    @Transaction
    @Query("SELECT COALESCE(MAX(`${PlaylistBean.ORDER_POSITION_COLUMN}`), 0) FROM $PLAYLIST_TABLE_NAME")
    suspend fun getMaxOrder(): Double

    @Transaction
    @Query("SELECT COALESCE(MIN(`${PlaylistBean.ORDER_POSITION_COLUMN}`), 0) FROM $PLAYLIST_TABLE_NAME")
    suspend fun getMinOrder(): Double

    @Transaction
    @Query(
        "SELECT `${PlaylistBean.ORDER_POSITION_COLUMN}` FROM $PLAYLIST_TABLE_NAME " +
                "WHERE ${PlaylistBean.PLAYLIST_ID_COLUMN} = :playlistId"
    )
    suspend fun getOrderPosition(playlistId: String): Double?

    @Transaction
    @Query(
        "SELECT EXISTS (SELECT 1 FROM $PLAYLIST_TABLE_NAME " +
                "WHERE ${PlaylistBean.PLAYLIST_ID_COLUMN} = :playlistId)"
    )
    suspend fun exists(playlistId: String): Int

    @Query(
        "SELECT MIN(${PlaylistBean.ORDER_POSITION_COLUMN}) FROM $PLAYLIST_TABLE_NAME WHERE " +
                "${PlaylistBean.ORDER_POSITION_COLUMN} > (" +
                "SELECT ${PlaylistBean.ORDER_POSITION_COLUMN} " +
                "FROM $PLAYLIST_TABLE_NAME " +
                "WHERE ${PlaylistBean.PLAYLIST_ID_COLUMN} = :playlistId" +
                ")"
    )
    suspend fun getNextOrderPosition(playlistId: String): Double?

    @Transaction
    @Query(
        "SELECT * FROM $PLAYLIST_TABLE_NAME " +
                "ORDER BY ${PlaylistBean.ORDER_POSITION_COLUMN} " +
                "LIMIT 1 OFFSET :index"
    )
    suspend fun getNth(index: Int): PlaylistBean?

    @Transaction
    @Query("SELECT * FROM $PLAYLIST_VIEW_NAME WHERE ${PlaylistBean.PLAYLIST_ID_COLUMN} = :playlistId")
    suspend fun getPlaylistView(playlistId: String): PlaylistViewBean

    @Transaction
    @RawQuery(observedEntities = [PlaylistViewBean::class])
    fun getPlaylistList(sql: RoomRawQuery): PagingSource<Int, PlaylistViewBean>

    fun getPlaylistList(
        orderByColumnName: String = PlaylistBean.ORDER_POSITION_COLUMN,
        asc: Boolean = true,
    ): PagingSource<Int, PlaylistViewBean> = getPlaylistList(
        RoomRawQuery(
            "SELECT * FROM $PLAYLIST_VIEW_NAME " +
                    "ORDER BY `$orderByColumnName` ${if (asc) "ASC" else "DESC"}"
        )
    )

    @Transaction
    @Query(
        "SELECT ${PlaylistBean.PLAYLIST_ID_COLUMN} FROM $PLAYLIST_TABLE_NAME " +
                "ORDER BY ${PlaylistBean.ORDER_POSITION_COLUMN}"
    )
    suspend fun getPlaylistIdList(): List<String>

    @Transaction
    suspend fun reindexOrders() {
        getPlaylistIdList().forEachIndexed { index, item ->
            reorderPlaylist(
                playlistId = item,
                orderPosition = (index * ORDER_DELTA) + ORDER_DELTA,
            )
        }
    }

    companion object {
        const val ORDER_DELTA = 10.0
        const val ORDER_MIN_DELTA = 0.05
    }
}