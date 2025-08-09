package com.skyd.podaura.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

class Migration1To2 : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
                CREATE TABLE DownloadLinkUuidMap (
                    link TEXT NOT NULL,
                    uuid TEXT NOT NULL,
                    PRIMARY KEY (link, uuid)
                )
                """
        )
    }
}