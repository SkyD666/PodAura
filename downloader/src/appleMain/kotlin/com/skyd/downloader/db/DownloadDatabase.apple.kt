package com.skyd.downloader.db

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.skyd.fundation.config.Const
import com.skyd.fundation.config.DB_DIR
import com.skyd.fundation.util.joinPath

actual fun DownloadDatabase.Companion.builder(): RoomDatabase.Builder<DownloadDatabase> {
    val dbPath = joinPath(Const.DB_DIR, DOWNLOAD_FILE_NAME)
    return Room.databaseBuilder<DownloadDatabase>(
        name = dbPath,
    ).setDriver(BundledSQLiteDriver())
}
