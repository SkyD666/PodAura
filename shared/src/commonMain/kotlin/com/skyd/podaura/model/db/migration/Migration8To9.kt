package com.skyd.podaura.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

class Migration8To9 : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE DownloadInfo_Backup (" +
                    "link TEXT PRIMARY KEY NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "name INTEGER NOT NULL, " +
                    "size INTEGER NOT NULL, " +
                    "progress REAL NOT NULL, " +
                    "progress TEXT, " +
                    "downloadState TEXT NOT NULL, " +
                    "downloadRequestId TEXT NOT NULL" +
                    ")"
        )
        connection.execSQL(
            "INSERT INTO DownloadInfo_Backup SELECT " +
                    "link, name, name, size, progress, " +
                    "progress, downloadState, downloadRequestId" +
                    " FROM DownloadInfo"
        )
        connection.execSQL("DROP TABLE DownloadInfo")
        connection.execSQL("ALTER TABLE DownloadInfo_Backup RENAME to DownloadInfo")
        connection.execSQL("CREATE UNIQUE INDEX index_DownloadInfo_link ON DownloadInfo (link)")
    }
}