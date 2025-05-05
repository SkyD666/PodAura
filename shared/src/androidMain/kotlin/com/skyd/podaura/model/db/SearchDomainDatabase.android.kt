package com.skyd.podaura.model.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.skyd.podaura.di.get

actual fun SearchDomainDatabase.Companion.builder(): RoomDatabase.Builder<SearchDomainDatabase> {
    val appContext = get<Context>()
    val dbFile = appContext.getDatabasePath(SEARCH_DOMAIN_DATA_BASE_FILE_NAME)
    return Room.databaseBuilder<SearchDomainDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    ).setDriver(AndroidSQLiteDriver())
}