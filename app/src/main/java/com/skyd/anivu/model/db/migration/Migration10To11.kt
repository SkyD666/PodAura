package com.skyd.anivu.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.skyd.anivu.model.bean.group.GROUP_TABLE_NAME
import com.skyd.anivu.model.bean.group.GroupBean
import com.skyd.anivu.model.bean.history.MEDIA_PLAY_HISTORY_TABLE_NAME
import com.skyd.anivu.model.bean.history.MediaPlayHistoryBean

class Migration10To11 : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE `$MEDIA_PLAY_HISTORY_TABLE_NAME` (" +
                    "${MediaPlayHistoryBean.PATH_COLUMN} TEXT NOT NULL PRIMARY KEY, " +
                    "${MediaPlayHistoryBean.LAST_PLAY_POSITION_COLUMN} INTEGER NOT NULL)"
        )
        db.execSQL("ALTER TABLE `$GROUP_TABLE_NAME` ADD ${GroupBean.IS_EXPANDED_COLUMN} INTEGER NOT NULL DEFAULT 1")
    }
}