package com.skyd.anivu.model.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.skyd.anivu.model.bean.download.bt.TORRENT_FILE_TABLE_NAME
import com.skyd.anivu.model.bean.download.bt.TorrentFileBean

@Dao
interface TorrentFileDao {
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateTorrentFile(torrentFileBean: TorrentFileBean)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateTorrentFiles(torrentFileList: List<TorrentFileBean>)

    @Transaction
    @Delete
    suspend fun deleteTorrentFile(torrentFileBean: TorrentFileBean): Int

    @Transaction
    @Query(
        """
        DELETE FROM $TORRENT_FILE_TABLE_NAME
        WHERE ${TorrentFileBean.LINK_COLUMN} = :link
        """
    )
    fun deleteTorrentFileByLink(link: String): Int

    @Transaction
    @Query(
        """
        SELECT * FROM $TORRENT_FILE_TABLE_NAME
        WHERE ${TorrentFileBean.LINK_COLUMN} = :link
        """
    )
    fun getTorrentFilesByLink(link: String): List<TorrentFileBean>

    @Transaction
    @Query(
        """
        SELECT COUNT(1) FROM $TORRENT_FILE_TABLE_NAME
        WHERE ${TorrentFileBean.PATH_COLUMN} = :path
        """
    )
    fun count(path: String): Int
}