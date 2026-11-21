package com.skyd.downloader.db

import androidx.room3.ColumnInfo
import androidx.room3.Entity

@Entity(
    tableName = DownloadBatchReceiptEntity.DOWNLOAD_BATCH_RECEIPT_TABLE_NAME,
    primaryKeys = [DownloadBatchReceiptEntity.BATCH_ID_COLUMN, DownloadBatchReceiptEntity.SUBMISSION_ID_COLUMN],
)
data class DownloadBatchReceiptEntity(
    @ColumnInfo(name = BATCH_ID_COLUMN) val batchId: String,
    @ColumnInfo(name = SUBMISSION_ID_COLUMN) val submissionId: String,
    @ColumnInfo(name = QUEUED_COUNT_COLUMN) val queuedCount: Int,
    @ColumnInfo(name = PAYLOAD_COLUMN) val payload: String,
) {
    companion object {
        const val DOWNLOAD_BATCH_RECEIPT_TABLE_NAME = "DownloadBatchReceipt"
        const val BATCH_ID_COLUMN = "batchId"
        const val SUBMISSION_ID_COLUMN = "submissionId"
        const val QUEUED_COUNT_COLUMN = "queuedCount"
        const val PAYLOAD_COLUMN = "payload"
    }
}
