package com.skyd.podaura.model.db.migration

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlinx.serialization.json.Json

class Migration28To29 : Migration(28, 29) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE ArticleNotificationRule ADD COLUMN feedUrls TEXT NOT NULL DEFAULT '[]'")
        connection.execSQL("ALTER TABLE ArticleNotificationRule ADD COLUMN groupIds TEXT NOT NULL DEFAULT '[]'")
        connection.execSQL("ALTER TABLE ArticleNotificationRule ADD COLUMN isManaged INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("CREATE INDEX index_ArticleNotificationRule_isManaged_feedUrls ON ArticleNotificationRule(isManaged, feedUrls)")
        connection.prepare("SELECT url, COALESCE(nickname, title, url) FROM Feed").use { feeds ->
            while (feeds.step()) {
                val url = feeds.getText(0)
                connection.prepare(
                    "INSERT INTO ArticleNotificationRule (name, regex, feedUrls, isManaged) VALUES (?, '', ?, 1)"
                ).use { insert ->
                    insert.bindText(1, feeds.getText(1))
                    insert.bindText(2, Json.encodeToString(listOf(url)))
                    insert.step()
                }
            }
        }
    }
}
