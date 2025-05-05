package com.skyd.podaura.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.skyd.podaura.model.bean.feed.FEED_TABLE_NAME
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.bean.group.GROUP_TABLE_NAME
import com.skyd.podaura.model.bean.group.GroupBean

class Migration3To4 : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
                CREATE TABLE `$GROUP_TABLE_NAME` (
                    ${GroupBean.GROUP_ID_COLUMN} TEXT NOT NULL PRIMARY KEY,
                    ${GroupBean.NAME_COLUMN} TEXT NOT NULL
                )
                """
        )
        connection.execSQL("ALTER TABLE $FEED_TABLE_NAME ADD ${FeedBean.GROUP_ID_COLUMN} TEXT")
        connection.execSQL("ALTER TABLE $FEED_TABLE_NAME ADD ${FeedBean.NICKNAME_COLUMN} TEXT")
    }
}