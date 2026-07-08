package com.skyd.downloader.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.migration.Migration


const val DOWNLOAD_FILE_NAME = "Downloader"

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
