package com.skyd.downloader.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: DownloadEntity)

    @Update
    suspend fun update(entity: DownloadEntity): Int

    @Query(
        "SELECT * FROM ${DownloadEntity.TABLE_NAME} " +
                "WHERE ${DownloadEntity.ID_COLUMN} = :id"
    )
    suspend fun find(id: String): DownloadEntity?

    @Query(
        "SELECT * FROM ${DownloadEntity.TABLE_NAME} " +
                "WHERE ${DownloadEntity.URL_COLUMN} = :url " +
                "AND ${DownloadEntity.PATH_COLUMN} = :path " +
                "AND ${DownloadEntity.REQUESTED_FILE_NAME_COLUMN} = :requestedFileName " +
                "ORDER BY ${DownloadEntity.CREATE_TIME_COLUMN} ASC LIMIT 1"
    )
    suspend fun findBySourceAndTarget(
        url: String,
        path: String,
        requestedFileName: String,
    ): DownloadEntity?

    @Query(
        "SELECT * FROM ${DownloadEntity.TABLE_NAME} " +
                "WHERE ${DownloadEntity.PATH_COLUMN} = :path " +
                "AND ${DownloadEntity.FILE_NAME_COLUMN} = :fileName " +
                "ORDER BY ${DownloadEntity.TIME_QUEUED_COLUMN} ASC LIMIT 1"
    )
    suspend fun findByDestination(path: String, fileName: String): DownloadEntity?

    @Query(
        "SELECT * FROM ${DownloadEntity.TABLE_NAME} " +
                "WHERE ${DownloadEntity.STATUS_COLUMN} IN (:statuses)"
    )
    suspend fun findAllInStatuses(statuses: List<String>): List<DownloadEntity>

    @Query(
        "SELECT COUNT(*) FROM ${DownloadEntity.TABLE_NAME} " +
                "WHERE ${DownloadEntity.STATUS_COLUMN} IN (:statuses)"
    )
    suspend fun countInStatuses(statuses: List<String>): Int

    @Query(
        "DELETE FROM ${DownloadEntity.TABLE_NAME} WHERE ${DownloadEntity.ID_COLUMN} = :id"
    )
    suspend fun remove(id: String)

    @Query(
        "SELECT * FROM ${DownloadEntity.TABLE_NAME} " +
                "ORDER BY ${DownloadEntity.TIME_QUEUED_COLUMN} ASC"
    )
    fun getAllEntityFlow(): Flow<List<DownloadEntity>>

    @Query(
        "SELECT * FROM ${DownloadEntity.TABLE_NAME} " +
                "ORDER BY ${DownloadEntity.TIME_QUEUED_COLUMN} ASC"
    )
    suspend fun getAllEntity(): List<DownloadEntity>

    @Query(
        "SELECT * FROM ${DownloadEntity.TABLE_NAME} " +
                "WHERE ${DownloadEntity.STATUS_COLUMN} = 'Success' " +
                "AND ${DownloadEntity.COMPLETION_HANDLED_COLUMN} = 0 " +
                "ORDER BY ${DownloadEntity.TIME_QUEUED_COLUMN} ASC"
    )
    fun observePendingCompletions(): Flow<List<DownloadEntity>>

    @Query(
        "SELECT * FROM ${DownloadEntity.TABLE_NAME} " +
                "WHERE ${DownloadEntity.STATUS_COLUMN} = 'Success' " +
                "AND ${DownloadEntity.COMPLETION_HANDLED_COLUMN} = 0 " +
                "ORDER BY ${DownloadEntity.TIME_QUEUED_COLUMN} ASC"
    )
    suspend fun getPendingCompletions(): List<DownloadEntity>

    @Query(
        "UPDATE ${DownloadEntity.TABLE_NAME} " +
                "SET ${DownloadEntity.STATUS_COLUMN} = 'Started', " +
                "${DownloadEntity.FAILURE_REASON_COLUMN} = '', " +
                "${DownloadEntity.FAILURE_CODE_COLUMN} = '', " +
                "${DownloadEntity.UPDATED_TIME_COLUMN} = :updatedTime " +
                "WHERE ${DownloadEntity.ID_COLUMN} = :id " +
                "AND ${DownloadEntity.ATTEMPT_ID_COLUMN} = :attemptId " +
                "AND ${DownloadEntity.STATUS_COLUMN} = 'Queued'"
    )
    suspend fun markStarted(id: String, attemptId: String, updatedTime: Long): Int

    @Query(
        "UPDATE ${DownloadEntity.TABLE_NAME} " +
                "SET ${DownloadEntity.FILE_NAME_COLUMN} = :fileName, " +
                "${DownloadEntity.FILE_NAME_RESOLVED_COLUMN} = 1, " +
                "${DownloadEntity.E_TAG_COLUMN} = :eTag, " +
                "${DownloadEntity.LAST_MODIFIED_COLUMN} = :lastModified, " +
                "${DownloadEntity.FINAL_URL_COLUMN} = :finalUrl, " +
                "${DownloadEntity.TOTAL_BYTES_COLUMN} = :totalBytes, " +
                "${DownloadEntity.UPDATED_TIME_COLUMN} = :updatedTime " +
                "WHERE ${DownloadEntity.ID_COLUMN} = :id " +
                "AND ${DownloadEntity.ATTEMPT_ID_COLUMN} = :attemptId " +
                "AND ${DownloadEntity.STATUS_COLUMN} IN ('Started', 'Downloading') " +
                "AND NOT EXISTS (" +
                "SELECT 1 FROM ${DownloadEntity.TABLE_NAME} AS owner " +
                "WHERE owner.${DownloadEntity.PATH_COLUMN} = :path " +
                "AND owner.${DownloadEntity.FILE_NAME_COLUMN} = :fileName " +
                "AND owner.${DownloadEntity.ID_COLUMN} != :id" +
                ")"
    )
    suspend fun claimResponseMetadata(
        id: String,
        attemptId: String,
        path: String,
        fileName: String,
        eTag: String,
        lastModified: String,
        finalUrl: String,
        totalBytes: Long,
        updatedTime: Long,
    ): Int

    @Query(
        "UPDATE ${DownloadEntity.TABLE_NAME} " +
                "SET ${DownloadEntity.STATUS_COLUMN} = 'Downloading', " +
                "${DownloadEntity.DOWNLOADED_BYTES_COLUMN} = :downloadedBytes, " +
                "${DownloadEntity.TOTAL_BYTES_COLUMN} = :totalBytes, " +
                "${DownloadEntity.SPEED_IN_BYTE_PER_MS_COLUMN} = :speedInBytePerMs, " +
                "${DownloadEntity.UPDATED_TIME_COLUMN} = :updatedTime " +
                "WHERE ${DownloadEntity.ID_COLUMN} = :id " +
                "AND ${DownloadEntity.ATTEMPT_ID_COLUMN} = :attemptId " +
                "AND ${DownloadEntity.STATUS_COLUMN} IN ('Started', 'Downloading')"
    )
    suspend fun updateProgress(
        id: String,
        attemptId: String,
        downloadedBytes: Long,
        totalBytes: Long,
        speedInBytePerMs: Float,
        updatedTime: Long,
    ): Int

    @Query(
        "UPDATE ${DownloadEntity.TABLE_NAME} " +
                "SET ${DownloadEntity.STATUS_COLUMN} = 'Success', " +
                "${DownloadEntity.DOWNLOADED_BYTES_COLUMN} = :totalBytes, " +
                "${DownloadEntity.TOTAL_BYTES_COLUMN} = :totalBytes, " +
                "${DownloadEntity.SPEED_IN_BYTE_PER_MS_COLUMN} = 0, " +
                "${DownloadEntity.COMPLETION_HANDLED_COLUMN} = 0, " +
                "${DownloadEntity.FAILURE_REASON_COLUMN} = '', " +
                "${DownloadEntity.FAILURE_CODE_COLUMN} = '', " +
                "${DownloadEntity.UPDATED_TIME_COLUMN} = :updatedTime " +
                "WHERE ${DownloadEntity.ID_COLUMN} = :id " +
                "AND ${DownloadEntity.ATTEMPT_ID_COLUMN} = :attemptId " +
                "AND ${DownloadEntity.STATUS_COLUMN} IN ('Started', 'Downloading')"
    )
    suspend fun markSuccess(
        id: String,
        attemptId: String,
        totalBytes: Long,
        updatedTime: Long,
    ): Int

    @Query(
        "UPDATE ${DownloadEntity.TABLE_NAME} " +
                "SET ${DownloadEntity.STATUS_COLUMN} = 'Queued', " +
                "${DownloadEntity.AUTO_RETRY_COUNT_COLUMN} = :retryCount, " +
                "${DownloadEntity.NEXT_ATTEMPT_AT_COLUMN} = :nextAttemptAt, " +
                "${DownloadEntity.SPEED_IN_BYTE_PER_MS_COLUMN} = 0, " +
                "${DownloadEntity.FAILURE_REASON_COLUMN} = :reason, " +
                "${DownloadEntity.FAILURE_CODE_COLUMN} = :code, " +
                "${DownloadEntity.UPDATED_TIME_COLUMN} = :updatedTime " +
                "WHERE ${DownloadEntity.ID_COLUMN} = :id " +
                "AND ${DownloadEntity.ATTEMPT_ID_COLUMN} = :attemptId " +
                "AND ${DownloadEntity.STATUS_COLUMN} IN ('Started', 'Downloading')"
    )
    suspend fun markRetryQueued(
        id: String,
        attemptId: String,
        retryCount: Int,
        nextAttemptAt: Long,
        reason: String,
        code: String,
        updatedTime: Long,
    ): Int

    @Query(
        "UPDATE ${DownloadEntity.TABLE_NAME} " +
                "SET ${DownloadEntity.STATUS_COLUMN} = 'Failed', " +
                "${DownloadEntity.SPEED_IN_BYTE_PER_MS_COLUMN} = 0, " +
                "${DownloadEntity.FAILURE_REASON_COLUMN} = :reason, " +
                "${DownloadEntity.FAILURE_CODE_COLUMN} = :code, " +
                "${DownloadEntity.UPDATED_TIME_COLUMN} = :updatedTime " +
                "WHERE ${DownloadEntity.ID_COLUMN} = :id " +
                "AND ${DownloadEntity.ATTEMPT_ID_COLUMN} = :attemptId " +
                "AND ${DownloadEntity.STATUS_COLUMN} IN ('Queued', 'Started', 'Downloading')"
    )
    suspend fun markFailed(
        id: String,
        attemptId: String,
        reason: String,
        code: String,
        updatedTime: Long,
    ): Int

    @Query(
        "UPDATE ${DownloadEntity.TABLE_NAME} " +
                "SET ${DownloadEntity.STATUS_COLUMN} = 'Queued', " +
                "${DownloadEntity.SPEED_IN_BYTE_PER_MS_COLUMN} = 0, " +
                "${DownloadEntity.UPDATED_TIME_COLUMN} = :updatedTime " +
                "WHERE ${DownloadEntity.ID_COLUMN} = :id " +
                "AND ${DownloadEntity.ATTEMPT_ID_COLUMN} = :attemptId " +
                "AND ${DownloadEntity.STATUS_COLUMN} IN ('Started', 'Downloading')"
    )
    suspend fun markInterruptedQueued(
        id: String,
        attemptId: String,
        updatedTime: Long,
    ): Int

    @Query(
        "UPDATE ${DownloadEntity.TABLE_NAME} " +
                "SET ${DownloadEntity.COMPLETION_HANDLED_COLUMN} = 1, " +
                "${DownloadEntity.UPDATED_TIME_COLUMN} = :updatedTime " +
                "WHERE ${DownloadEntity.ID_COLUMN} = :id " +
                "AND ${DownloadEntity.STATUS_COLUMN} = 'Success'"
    )
    suspend fun markCompletionHandled(id: String, updatedTime: Long): Int
}
