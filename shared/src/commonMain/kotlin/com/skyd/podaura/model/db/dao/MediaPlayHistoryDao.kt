package com.skyd.podaura.model.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import androidx.room.Transaction
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.bean.history.MEDIA_PLAY_HISTORY_TABLE_NAME
import com.skyd.podaura.model.bean.history.MediaPlayHistoryBean
import com.skyd.podaura.model.bean.history.MediaPlayHistoryWithArticle

@Dao
interface MediaPlayHistoryDao {
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateMediaPlayHistory(mediaPlayHistoryBean: MediaPlayHistoryBean)

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
    suspend fun getMediaPlayHistory(path: String): MediaPlayHistoryBean?

    @Transaction
    @Query("SELECT * FROM $MEDIA_PLAY_HISTORY_TABLE_NAME ORDER BY ${MediaPlayHistoryBean.LAST_TIME_COLUMN} DESC")
    fun getMediaPlayHistoryList(): PagingSource<Int, MediaPlayHistoryWithArticle>

    @Transaction
    @RawQuery(observedEntities = [MediaPlayHistoryBean::class, FeedBean::class, ArticleBean::class])
    fun searchMediaPlayHistoryList(sql: RoomRawQuery): PagingSource<Int, MediaPlayHistoryWithArticle>

    @Transaction
    @Query(
        """
        UPDATE $MEDIA_PLAY_HISTORY_TABLE_NAME
        SET ${MediaPlayHistoryBean.LAST_PLAY_POSITION_COLUMN} = :lastPlayPosition
        WHERE ${MediaPlayHistoryBean.PATH_COLUMN} = :path
        """
    )
    suspend fun updateLastPlayPosition(path: String, lastPlayPosition: Long): Int
}