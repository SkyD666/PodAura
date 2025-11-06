package com.skyd.podaura.model.db

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.skyd.fundation.config.Const
import com.skyd.fundation.config.DB_DIR
import java.io.File

actual fun AppDatabase.Companion.builder(): RoomDatabase.Builder<AppDatabase> {
    val dbFile = File(Const.DB_DIR, APP_DATA_BASE_FILE_NAME)
    return Room.databaseBuilder<AppDatabase>(
        name = dbFile.absolutePath,
    ).setDriver(BundledSQLiteDriver())
}