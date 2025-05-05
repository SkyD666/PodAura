package com.skyd.podaura.model.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import com.skyd.podaura.model.bean.SearchDomainBean
import com.skyd.podaura.model.db.dao.SearchDomainDao

const val SEARCH_DOMAIN_DATA_BASE_FILE_NAME = "searchDomain.db"

@Database(
    entities = [
        SearchDomainBean::class,
    ],
    version = 1
)
@TypeConverters(
    value = []
)
abstract class SearchDomainDatabase : RoomDatabase() {

    abstract fun searchDomainDao(): SearchDomainDao

    companion object {
        @Volatile
        private var instance: SearchDomainDatabase? = null

        private val migrations = arrayOf<Migration>()

        fun getInstance(context: Context): SearchDomainDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SearchDomainDatabase::class.java,
                    SEARCH_DOMAIN_DATA_BASE_FILE_NAME
                )
                    .addMigrations(*migrations)
                    .build()
                    .apply { instance = this }
            }
        }
    }
}
