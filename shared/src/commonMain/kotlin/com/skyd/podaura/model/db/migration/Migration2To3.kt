package com.skyd.podaura.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

class Migration2To3 : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
                CREATE TABLE TorrentFile (
                    link TEXT NOT NULL,
                    path TEXT NOT NULL,
                    size INTEGER NOT NULL,
                    PRIMARY KEY (link, path)
                    FOREIGN KEY (link)
                                REFERENCES DownloadInfo(link)
                                ON DELETE CASCADE
                )
                """
        )
    }
}