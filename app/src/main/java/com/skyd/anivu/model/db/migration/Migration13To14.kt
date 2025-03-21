package com.skyd.anivu.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.skyd.anivu.model.bean.ARTICLE_NOTIFICATION_RULE_TABLE_NAME
import com.skyd.anivu.model.bean.ArticleNotificationRuleBean

class Migration13To14 : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
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