package com.skyd.podaura.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.skyd.podaura.model.bean.feed.FEED_TABLE_NAME
import com.skyd.podaura.model.bean.feed.FeedBean

class Migration16To17 : Migration(16, 17) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `$FEED_TABLE_NAME` ADD ${FeedBean.MUTE_COLUMN} INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE `$FEED_TABLE_NAME` ADD previousFeedUrl TEXT")
        connection.execSQL("ALTER TABLE `$FEED_TABLE_NAME` ADD nextFeedUrl TEXT")
    }
}