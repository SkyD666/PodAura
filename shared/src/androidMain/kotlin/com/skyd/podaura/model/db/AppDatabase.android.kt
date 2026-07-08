package com.skyd.podaura.model.db

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.skyd.fundation.di.get

actual fun AppDatabase.Companion.builder(): RoomDatabase.Builder<AppDatabase> {
    val appContext = get<Context>()
    val dbFile = appContext.getDatabasePath(APP_DATA_BASE_FILE_NAME)
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    ).setDriver(AndroidSQLiteDriver())
}
