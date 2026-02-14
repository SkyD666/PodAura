package com.skyd.podaura.model.db

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.skyd.fundation.config.Const
import com.skyd.fundation.config.DB_DIR
import com.skyd.fundation.util.joinPath

actual fun SearchDomainDatabase.Companion.builder(): RoomDatabase.Builder<SearchDomainDatabase> {
    val dbPath = joinPath(Const.DB_DIR, SEARCH_DOMAIN_DATA_BASE_FILE_NAME)
    return Room.databaseBuilder<SearchDomainDatabase>(
        name = dbPath
    ).setDriver(BundledSQLiteDriver())
}
