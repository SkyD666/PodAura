package com.skyd.downloader.db

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

class Migration1To2 : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE `${DownloadEntity.TABLE_NAME}` ADD COLUMN `metadata` TEXT"
        )
    }
}
