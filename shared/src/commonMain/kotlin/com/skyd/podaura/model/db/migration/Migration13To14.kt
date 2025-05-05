package com.skyd.podaura.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.skyd.podaura.model.bean.ARTICLE_NOTIFICATION_RULE_TABLE_NAME
import com.skyd.podaura.model.bean.ArticleNotificationRuleBean

class Migration13To14 : Migration(13, 14) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
                CREATE TABLE `$ARTICLE_NOTIFICATION_RULE_TABLE_NAME` (
                    ${ArticleNotificationRuleBean.ID_COLUMN} INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    ${ArticleNotificationRuleBean.NAME_COLUMN} TEXT NOT NULL,
                    ${ArticleNotificationRuleBean.REGEX_COLUMN} TEXT NOT NULL
                )
                """
        )
    }
}