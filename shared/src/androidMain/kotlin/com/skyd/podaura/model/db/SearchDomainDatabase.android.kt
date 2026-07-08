package com.skyd.podaura.model.db

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.skyd.fundation.di.get

actual fun SearchDomainDatabase.Companion.builder(): RoomDatabase.Builder<SearchDomainDatabase> {
    val appContext = get<Context>()
    val dbFile = appContext.getDatabasePath(SEARCH_DOMAIN_DATA_BASE_FILE_NAME)
    return Room.databaseBuilder<SearchDomainDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    ).setDriver(AndroidSQLiteDriver())
}
