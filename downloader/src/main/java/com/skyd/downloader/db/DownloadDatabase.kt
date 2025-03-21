package com.skyd.downloader.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DownloadEntity::class], version = 1)
internal abstract class DownloadDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
}
