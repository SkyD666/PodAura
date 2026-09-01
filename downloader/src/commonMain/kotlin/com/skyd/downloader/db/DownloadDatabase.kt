package com.skyd.downloader.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor


const val DOWNLOAD_FILE_NAME = "Downloader"

// The Room compiler generates the `actual` implementations.
@Suppress("KotlinNoActualForExpect")
expect object DownloadDatabaseConstructor : RoomDatabaseConstructor<DownloadDatabase> {
    override fun initialize(): DownloadDatabase
}

expect fun DownloadDatabase.Companion.builder(): RoomDatabase.Builder<DownloadDatabase>

@ConstructedBy(DownloadDatabaseConstructor::class)
@Database(entities = [DownloadEntity::class], version = 3, exportSchema = true)
abstract class DownloadDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao

    companion object
}

fun DownloadDatabase.Companion.instance(
    builder: RoomDatabase.Builder<DownloadDatabase>
): DownloadDatabase {
    val migrations = arrayOf(Migration1To2(), Migration2To3())

    return builder
        .addMigrations(*migrations)
        .build()
}
