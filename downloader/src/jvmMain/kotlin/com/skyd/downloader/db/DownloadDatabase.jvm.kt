package com.skyd.downloader.db

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File

actual fun DownloadDatabase.Companion.builder(): RoomDatabase.Builder<DownloadDatabase> {
    val dbFile = File(System.getProperty("java.io.tmpdir"), DOWNLOAD_FILE_NAME)
    return Room.databaseBuilder<DownloadDatabase>(
        name = dbFile.absolutePath,
    ).setDriver(BundledSQLiteDriver())
}