package com.skyd.anivu.model.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.skyd.anivu.model.bean.download.bt.DOWNLOAD_INFO_TABLE_NAME
import com.skyd.anivu.model.bean.download.bt.DOWNLOAD_LINK_UUID_MAP_TABLE_NAME
import com.skyd.anivu.model.bean.download.bt.BtDownloadInfoBean
import com.skyd.anivu.model.bean.download.bt.DownloadLinkUuidMapBean
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadInfoDao {
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateDownloadInfo(downloadInfo: BtDownloadInfoBean)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateDownloadInfo(btDownloadInfoBeanList: List<BtDownloadInfoBean>)

    @Transaction
    @Query(
        """
        UPDATE $DOWNLOAD_INFO_TABLE_NAME
        SET ${BtDownloadInfoBean.DOWNLOAD_REQUEST_ID_COLUMN} = :downloadRequestId
        WHERE ${BtDownloadInfoBean.LINK_COLUMN} = :link
        """
    )
    fun updateDownloadInfoRequestId(
        link: String,
        downloadRequestId: String,
    ): Int

    @Transaction
    @Query(
        """
        UPDATE $DOWNLOAD_INFO_TABLE_NAME
        SET ${BtDownloadInfoBean.NAME_COLUMN} = :name,
            ${BtDownloadInfoBean.SIZE_COLUMN} = :size,
            ${BtDownloadInfoBean.PROGRESS_COLUMN} = :progress
        WHERE ${BtDownloadInfoBean.LINK_COLUMN} = :link
        """
    )
    fun updateDownloadInfo(
        link: String,
        name: String,
        size: Long,
        progress: Float,
    )

    @Transaction
    @Delete
    suspend fun deleteDownloadInfo(btDownloadInfoBean: BtDownloadInfoBean): Int

    @Transaction
    @Query(
        """
        DELETE FROM $DOWNLOAD_INFO_TABLE_NAME
        WHERE ${BtDownloadInfoBean.LINK_COLUMN} = :link
        """
    )
    suspend fun deleteDownloadInfo(
        link: String,
    ): Int

    @Transaction
    @Query(
        """
        UPDATE $DOWNLOAD_INFO_TABLE_NAME
        SET ${BtDownloadInfoBean.DOWNLOAD_STATE_COLUMN} = :downloadState
        WHERE ${BtDownloadInfoBean.LINK_COLUMN} = :link
        """
    )
    fun updateDownloadState(
        link: String,
        downloadState: BtDownloadInfoBean.DownloadState,
    ): Int

    @Transaction
    @Query(
        """
        SELECT ${BtDownloadInfoBean.DOWNLOAD_STATE_COLUMN} FROM $DOWNLOAD_INFO_TABLE_NAME
        WHERE ${BtDownloadInfoBean.LINK_COLUMN} = :link
        """
    )
    fun getDownloadState(
        link: String,
    ): BtDownloadInfoBean.DownloadState?

    @Transaction
    @Query(
        """
        SELECT * FROM $DOWNLOAD_INFO_TABLE_NAME
        WHERE ${BtDownloadInfoBean.LINK_COLUMN} = :link
        """
    )
    fun getDownloadInfo(
        link: String,
    ): BtDownloadInfoBean?

    @Transaction
    @Query(
        """
        SELECT ${BtDownloadInfoBean.DESCRIPTION_COLUMN} FROM $DOWNLOAD_INFO_TABLE_NAME
        WHERE ${BtDownloadInfoBean.LINK_COLUMN} = :link
        """
    )
    fun getDownloadDescription(
        link: String,
    ): String?

    @Transaction
    @Query(
        """
        UPDATE $DOWNLOAD_INFO_TABLE_NAME
        SET ${BtDownloadInfoBean.DESCRIPTION_COLUMN} = :description
        WHERE ${BtDownloadInfoBean.LINK_COLUMN} = :link
        """
    )
    fun updateDownloadDescription(
        link: String,
        description: String?,
    ): Int

    @Transaction
    @Query(
        """
        SELECT ${BtDownloadInfoBean.SIZE_COLUMN} FROM $DOWNLOAD_INFO_TABLE_NAME
        WHERE ${BtDownloadInfoBean.LINK_COLUMN} = :link
        """
    )
    fun getDownloadSize(
        link: String,
    ): Long?

    @Transaction
    @Query(
        """
        UPDATE $DOWNLOAD_INFO_TABLE_NAME
        SET ${BtDownloadInfoBean.SIZE_COLUMN} = :size
        WHERE ${BtDownloadInfoBean.LINK_COLUMN} = :link
        """
    )
    fun updateDownloadSize(
        link: String,
        size: Long,
    ): Int

    @Transaction
    @Query(
        """
        SELECT ${BtDownloadInfoBean.PROGRESS_COLUMN} FROM $DOWNLOAD_INFO_TABLE_NAME
        WHERE ${BtDownloadInfoBean.LINK_COLUMN} = :link
        """
    )
    fun getDownloadProgress(
        link: String,
    ): Float?

    @Transaction
    @Query(
        """
        UPDATE $DOWNLOAD_INFO_TABLE_NAME
        SET ${BtDownloadInfoBean.PROGRESS_COLUMN} = :progress
        WHERE ${BtDownloadInfoBean.LINK_COLUMN} = :link
        """
    )
    fun updateDownloadProgress(
        link: String,
        progress: Float,
    ): Int

    @Transaction
    @Query(
        """
        SELECT ${BtDownloadInfoBean.NAME_COLUMN} FROM $DOWNLOAD_INFO_TABLE_NAME
        WHERE ${BtDownloadInfoBean.LINK_COLUMN} = :link
        """
    )
    fun getDownloadName(
        link: String,
    ): String?

    @Transaction
    @Query(
        """
        UPDATE $DOWNLOAD_INFO_TABLE_NAME
        SET ${BtDownloadInfoBean.NAME_COLUMN} = :name
        WHERE ${BtDownloadInfoBean.LINK_COLUMN} = :link
        """
    )
    fun updateDownloadName(
        link: String,
        name: String,
    ): Int

    @Transaction
    @Query(
        """
        SELECT COUNT(1) FROM $DOWNLOAD_INFO_TABLE_NAME
        WHERE ${BtDownloadInfoBean.LINK_COLUMN} = :link
        """
    )
    fun containsDownloadInfo(
        link: String,
    ): Int

    @Transaction
    @Query(
        """
        SELECT * FROM $DOWNLOAD_INFO_TABLE_NAME
        WHERE ${BtDownloadInfoBean.PROGRESS_COLUMN} < 1
        AND ${BtDownloadInfoBean.DOWNLOAD_STATE_COLUMN} <> :completedState
        """
    )
    fun getDownloadingListFlow(
        completedState: String = BtDownloadInfoBean.DownloadState.Completed.name
    ): Flow<List<BtDownloadInfoBean>>

    @Transaction
    @Query(
        """
        SELECT * FROM $DOWNLOAD_INFO_TABLE_NAME
        """
    )
    fun getAllDownloadListFlow(): Flow<List<BtDownloadInfoBean>>

    @Transaction
    @Query(
        """
        SELECT ${BtDownloadInfoBean.DOWNLOAD_REQUEST_ID_COLUMN} FROM $DOWNLOAD_INFO_TABLE_NAME
        """
    )
    fun getAllDownloadRequestIdFlow(): Flow<List<String>>

    @Transaction
    @Query("SELECT * FROM $DOWNLOAD_INFO_TABLE_NAME WHERE ${BtDownloadInfoBean.PROGRESS_COLUMN} < 1")
    fun getDownloadingList(): List<BtDownloadInfoBean>

    @Transaction
    @Query("SELECT * FROM $DOWNLOAD_INFO_TABLE_NAME WHERE ${BtDownloadInfoBean.PROGRESS_COLUMN} == 1")
    fun getDownloadedList(): Flow<List<BtDownloadInfoBean>>

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setDownloadLinkUuidMap(bean: DownloadLinkUuidMapBean)

    @Transaction
    @Query(
        """
        SELECT ${DownloadLinkUuidMapBean.LINK_COLUMN} FROM $DOWNLOAD_LINK_UUID_MAP_TABLE_NAME
        WHERE ${DownloadLinkUuidMapBean.UUID_COLUMN} == :uuid
        """
    )
    fun getDownloadLinkByUuid(uuid: String): String?

    @Transaction
    @Query(
        """
        SELECT ${DownloadLinkUuidMapBean.UUID_COLUMN} FROM $DOWNLOAD_LINK_UUID_MAP_TABLE_NAME
        WHERE ${DownloadLinkUuidMapBean.LINK_COLUMN} == :link
        """
    )
    fun getDownloadUuidByLink(link: String): String?

    @Transaction
    @Query(
        """
        DELETE FROM $DOWNLOAD_LINK_UUID_MAP_TABLE_NAME
        WHERE ${DownloadLinkUuidMapBean.UUID_COLUMN} == :uuid
        """
    )
    fun removeDownloadLinkByUuid(uuid: String): Int

    @Transaction
    @Query(
        """
        DELETE FROM $DOWNLOAD_LINK_UUID_MAP_TABLE_NAME
        WHERE ${DownloadLinkUuidMapBean.LINK_COLUMN} == :link
        """
    )
    fun removeDownloadLinkUuidMap(link: String): Int
}