package com.skyd.downloader.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
interface DownloadBatchReceiptDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(receipt: DownloadBatchReceiptEntity)

    @Query(
        "SELECT * FROM ${DownloadBatchReceiptEntity.DOWNLOAD_BATCH_RECEIPT_TABLE_NAME} " +
                "WHERE ${DownloadBatchReceiptEntity.BATCH_ID_COLUMN} = :batchId " +
                "AND ${DownloadBatchReceiptEntity.SUBMISSION_ID_COLUMN} = :submissionId"
    )
    suspend fun find(batchId: String, submissionId: String): DownloadBatchReceiptEntity?

    @Query(
        "SELECT * FROM ${DownloadBatchReceiptEntity.DOWNLOAD_BATCH_RECEIPT_TABLE_NAME} " +
                "WHERE ${DownloadBatchReceiptEntity.BATCH_ID_COLUMN} = :batchId " +
                "AND ${DownloadBatchReceiptEntity.QUEUED_COUNT_COLUMN} > 0"
    )
    suspend fun findQueued(batchId: String): List<DownloadBatchReceiptEntity>
}
