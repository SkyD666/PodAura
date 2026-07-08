package com.skyd.downloader.db

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.skyd.fundation.di.get

actual fun DownloadDatabase.Companion.builder(): RoomDatabase.Builder<DownloadDatabase> {
    val appContext = get<Context>()
    val dbFile = appContext.getDatabasePath(DOWNLOAD_FILE_NAME)
    return Room.databaseBuilder<DownloadDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    ).setDriver(AndroidSQLiteDriver())
}
