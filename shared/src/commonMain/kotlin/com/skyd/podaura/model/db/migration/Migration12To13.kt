package com.skyd.podaura.model.db.migration

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.skyd.podaura.model.bean.feed.FEED_TABLE_NAME
import com.skyd.podaura.model.bean.feed.FeedBean

class Migration12To13 : Migration(12, 13) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE $FEED_TABLE_NAME ADD ${FeedBean.REQUEST_HEADERS_COLUMN} TEXT")
    }
}
