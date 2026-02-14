package com.skyd.podaura.model.db

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.skyd.fundation.config.Const
import com.skyd.fundation.config.DB_DIR
import com.skyd.fundation.util.joinPath

actual fun AppDatabase.Companion.builder(): RoomDatabase.Builder<AppDatabase> {
    val dbPath = joinPath(Const.DB_DIR, APP_DATA_BASE_FILE_NAME)
    return Room.databaseBuilder<AppDatabase>(
        name = dbPath
    ).setDriver(BundledSQLiteDriver())
}
