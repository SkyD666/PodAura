package com.skyd.downloader.db

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

class Migration2To3 : Migration(2, 3) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `Download_v3` (
                `id` TEXT NOT NULL,
                `url` TEXT NOT NULL,
                `path` TEXT NOT NULL,
                `fileName` TEXT NOT NULL,
                `requestedFileName` TEXT NOT NULL,
                `fileNameResolved` INTEGER NOT NULL,
                `timeQueued` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `totalBytes` INTEGER NOT NULL,
                `downloadedBytes` INTEGER NOT NULL,
                `speedInBytePerMs` REAL NOT NULL,
                `eTag` TEXT NOT NULL,
                `lastModified` TEXT NOT NULL,
                `finalUrl` TEXT NOT NULL,
                `attemptId` TEXT NOT NULL,
                `autoRetryCount` INTEGER NOT NULL,
                `nextAttemptAt` INTEGER NOT NULL,
                `createTime` INTEGER NOT NULL,
                `updatedTime` INTEGER NOT NULL,
                `failureReason` TEXT NOT NULL,
                `failureCode` TEXT NOT NULL,
                `metadata` TEXT,
                `completionHandled` INTEGER NOT NULL,
                `requireUnmetered` INTEGER NOT NULL,
                `requiresCharging` INTEGER NOT NULL,
                `requiresBatteryNotLow` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        connection.execSQL(
            """
            INSERT INTO `Download_v3` (
                `id`, `url`, `path`, `fileName`, `requestedFileName`, `fileNameResolved`,
                `timeQueued`, `status`, `totalBytes`, `downloadedBytes`,
                `speedInBytePerMs`, `eTag`, `lastModified`, `finalUrl`, `attemptId`,
                `autoRetryCount`, `nextAttemptAt`, `createTime`,
                `updatedTime`, `failureReason`, `failureCode`, `metadata`,
                `completionHandled`, `requireUnmetered`,
                `requiresCharging`, `requiresBatteryNotLow`
            )
            SELECT
                'legacy:' || `id`, `url`, `path`, `fileName`, `fileName`, 1,
                `timeQueued`,
                CASE
                    WHEN `status` IN ('Init', 'Queued', 'Started', 'Downloading') THEN 'Paused'
                    ELSE `status`
                END,
                `totalBytes`, `downloadedBytes`, `speedInBytePerMs`, `eTag`, '', '', '',
                0, 0,
                CASE WHEN `createTime` = 0 THEN `timeQueued` ELSE `createTime` END,
                CASE WHEN `createTime` = 0 THEN `timeQueued` ELSE `createTime` END,
                `failureReason`, '', `metadata`,
                CASE WHEN `status` = 'Success' THEN 1 ELSE 0 END,
                0, 0, 0
            FROM `Download`
            """.trimIndent()
        )
        connection.execSQL("DROP TABLE `Download`")
        connection.execSQL("ALTER TABLE `Download_v3` RENAME TO `Download`")
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_Download_url_path_requestedFileName` " +
                    "ON `Download` (`url`, `path`, `requestedFileName`)"
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_Download_path_fileName` " +
                    "ON `Download` (`path`, `fileName`)"
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_Download_status` ON `Download` (`status`)"
        )
    }
}
