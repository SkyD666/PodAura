package com.skyd.podaura.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.skyd.podaura.model.bean.feed.FEED_TABLE_NAME
import com.skyd.podaura.model.bean.feed.FeedBean

class Migration7To8 : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE $FEED_TABLE_NAME ADD ${FeedBean.SORT_XML_ARTICLES_ON_UPDATE_COLUMN} INTEGER NOT NULL DEFAULT 0")
    }
}