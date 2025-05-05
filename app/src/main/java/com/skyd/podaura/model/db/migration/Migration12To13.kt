package com.skyd.podaura.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.skyd.podaura.model.bean.feed.FEED_TABLE_NAME
import com.skyd.podaura.model.bean.feed.FeedBean

class Migration12To13 : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE $FEED_TABLE_NAME ADD ${FeedBean.REQUEST_HEADERS_COLUMN} TEXT")
    }
}