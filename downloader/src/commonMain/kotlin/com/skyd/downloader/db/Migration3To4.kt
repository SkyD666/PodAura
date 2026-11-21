package com.skyd.downloader.db

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.skyd.downloader.db.DownloadBatchReceiptEntity.Companion.BATCH_ID_COLUMN
import com.skyd.downloader.db.DownloadBatchReceiptEntity.Companion.DOWNLOAD_BATCH_RECEIPT_TABLE_NAME
import com.skyd.downloader.db.DownloadBatchReceiptEntity.Companion.PAYLOAD_COLUMN
import com.skyd.downloader.db.DownloadBatchReceiptEntity.Companion.QUEUED_COUNT_COLUMN
import com.skyd.downloader.db.DownloadBatchReceiptEntity.Companion.SUBMISSION_ID_COLUMN

class Migration3To4 : Migration(3, 4) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE `${DownloadEntity.TABLE_NAME}` ADD COLUMN `${DownloadEntity.BATCH_ID_COLUMN}` TEXT"
        )
        connection.execSQL(
            "ALTER TABLE `${DownloadEntity.TABLE_NAME}` " +
                    "ADD COLUMN `${DownloadEntity.REQUIRE_FILE_RESPONSE_COLUMN}` INTEGER NOT NULL DEFAULT 0"
        )
        connection.execSQL(
            "ALTER TABLE `${DownloadEntity.TABLE_NAME}` ADD COLUMN `${DownloadEntity.PUBLICATION_DIGEST_COLUMN}` TEXT"
        )
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `$DOWNLOAD_BATCH_RECEIPT_TABLE_NAME` (
                `$BATCH_ID_COLUMN` TEXT NOT NULL,
                `$SUBMISSION_ID_COLUMN` TEXT NOT NULL,
                `$QUEUED_COUNT_COLUMN` INTEGER NOT NULL,
                `$PAYLOAD_COLUMN` TEXT NOT NULL,
                PRIMARY KEY(`$BATCH_ID_COLUMN`, `$SUBMISSION_ID_COLUMN`)
            )
            """.trimIndent()
        )
    }
}
