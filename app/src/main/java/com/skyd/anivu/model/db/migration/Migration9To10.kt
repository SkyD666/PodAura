package com.skyd.anivu.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.skyd.anivu.model.bean.group.GROUP_TABLE_NAME

class Migration9To10 : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `$GROUP_TABLE_NAME` ADD previousGroupId TEXT")
        db.execSQL("ALTER TABLE `$GROUP_TABLE_NAME` ADD nextGroupId TEXT")
    }
}