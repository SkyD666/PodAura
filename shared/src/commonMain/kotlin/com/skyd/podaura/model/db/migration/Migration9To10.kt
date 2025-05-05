package com.skyd.podaura.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.skyd.podaura.model.bean.group.GROUP_TABLE_NAME

class Migration9To10 : Migration(9, 10) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `$GROUP_TABLE_NAME` ADD previousGroupId TEXT")
        connection.execSQL("ALTER TABLE `$GROUP_TABLE_NAME` ADD nextGroupId TEXT")
    }
}