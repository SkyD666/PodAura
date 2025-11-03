package com.skyd.downloader.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration


const val APP_DATA_BASE_FILE_NAME = "Downloader"

// The Room compiler generates the `actual` implementations.
@Suppress("KotlinNoActualForExpect")
expect object DownloadDatabaseConstructor : RoomDatabaseConstructor<DownloadDatabase> {
    override fun initialize(): DownloadDatabase
}

expect fun DownloadDatabase.Companion.builder(): RoomDatabase.Builder<DownloadDatabase>

@ConstructedBy(DownloadDatabaseConstructor::class)
@Database(entities = [DownloadEntity::class], version = 1)
abstract class DownloadDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao

    companion object
}

fun DownloadDatabase.Companion.instance(
    builder: RoomDatabase.Builder<DownloadDatabase>
): DownloadDatabase {
    val migrations = arrayOf<Migration>()

    return builder
        .addMigrations(*migrations)
        .build()
}