package com.skyd.anivu.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.skyd.anivu.model.bean.feed.FEED_TABLE_NAME
import com.skyd.anivu.model.bean.feed.FeedBean

class Migration16To17 : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `$FEED_TABLE_NAME` ADD ${FeedBean.MUTE_COLUMN} INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `$FEED_TABLE_NAME` ADD previousFeedUrl TEXT")
        db.execSQL("ALTER TABLE `$FEED_TABLE_NAME` ADD nextFeedUrl TEXT")
    }
}