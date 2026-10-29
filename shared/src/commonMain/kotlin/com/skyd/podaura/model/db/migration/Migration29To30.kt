package com.skyd.podaura.model.db.migration

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

class Migration29To30 : Migration(29, 30) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE Playlist ADD COLUMN mediaSortBy TEXT")
        connection.execSQL("ALTER TABLE Playlist ADD COLUMN mediaSortAsc INTEGER NOT NULL DEFAULT 1")
    }
}
