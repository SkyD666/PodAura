package com.skyd.anivu.model.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.skyd.anivu.model.bean.MEDIA_PLAY_HISTORY_TABLE_NAME
import com.skyd.anivu.model.bean.MediaPlayHistoryBean

@Dao
interface MediaPlayHistoryDao {
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateMediaPlayHistory(mediaPlayHistoryBean: MediaPlayHistoryBean)

    @Transaction
    @Query(
        """
        DELETE FROM $MEDIA_PLAY_HISTORY_TABLE_NAME
        WHERE ${MediaPlayHistoryBean.PATH_COLUMN} = :path
        """
    )
    suspend fun deleteMediaPlayHistory(path: String): Int

    @Transaction
    @Query("DELETE FROM $MEDIA_PLAY_HISTORY_TABLE_NAME")
    suspend fun deleteAllMediaPlayHistory(): Int

    @Transaction
    @Query("SELECT * FROM $MEDIA_PLAY_HISTORY_TABLE_NAME WHERE ${MediaPlayHistoryBean.PATH_COLUMN} = :path")
    fun getMediaPlayHistory(path: String): MediaPlayHistoryBean?

    @Transaction
    @Query("SELECT * FROM $MEDIA_PLAY_HISTORY_TABLE_NAME")
    fun getMediaPlayHistoryList(): PagingSource<Int, MediaPlayHistoryBean>

    @Transaction
    @Query(
        """
        UPDATE $MEDIA_PLAY_HISTORY_TABLE_NAME
        SET ${MediaPlayHistoryBean.LAST_PLAY_POSITION_COLUMN} = :lastPlayPosition
        WHERE ${MediaPlayHistoryBean.PATH_COLUMN} = :path
        """
    )
    fun updateLastPlayPosition(path: String, lastPlayPosition: Long): Int
}