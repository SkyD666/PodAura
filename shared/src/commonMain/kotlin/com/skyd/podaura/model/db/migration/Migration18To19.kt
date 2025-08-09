package com.skyd.podaura.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.model.preference.data.medialib.MediaLibLocationPreference
import com.skyd.podaura.model.preference.dataStore

class Migration18To19 : Migration(18, 19) {
    override fun migrate(connection: SQLiteConnection) {
        val defaultPath = dataStore.getOrDefault(MediaLibLocationPreference)
        connection.execSQL("ALTER TABLE `DownloadInfo` ADD path TEXT NOT NULL DEFAULT \"$defaultPath\"")
    }
}