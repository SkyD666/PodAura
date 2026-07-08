package com.skyd.podaura.model.db.migration

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

class Migration25To26 : Migration(25, 26) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `DownloadInfo`")
        connection.execSQL("DROP TABLE IF EXISTS `DownloadLinkUuidMap`")
        connection.execSQL("DROP TABLE IF EXISTS `SessionParams`")
        connection.execSQL("DROP TABLE IF EXISTS `TorrentFile`")
    }
}
