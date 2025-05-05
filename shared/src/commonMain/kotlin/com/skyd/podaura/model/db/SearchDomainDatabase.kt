package com.skyd.podaura.model.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.skyd.podaura.model.bean.SearchDomainBean
import com.skyd.podaura.model.db.dao.SearchDomainDao

const val SEARCH_DOMAIN_DATA_BASE_FILE_NAME = "searchDomain.db"

// The Room compiler generates the `actual` implementations.
@Suppress("KotlinNoActualForExpect")
expect object SearchDomainDatabaseConstructor : RoomDatabaseConstructor<SearchDomainDatabase> {
    override fun initialize(): SearchDomainDatabase
}

@Database(
    entities = [
        SearchDomainBean::class,
    ],
    version = 1
)
@ConstructedBy(SearchDomainDatabaseConstructor::class)
@TypeConverters(
    value = []
)
abstract class SearchDomainDatabase : RoomDatabase() {

    abstract fun searchDomainDao(): SearchDomainDao

    companion object
}

expect fun SearchDomainDatabase.Companion.builder(): RoomDatabase.Builder<SearchDomainDatabase>

fun SearchDomainDatabase.Companion.instance(
    builder: RoomDatabase.Builder<SearchDomainDatabase>
): SearchDomainDatabase {
    return builder.build()
}