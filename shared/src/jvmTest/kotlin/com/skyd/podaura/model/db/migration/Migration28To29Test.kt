package com.skyd.podaura.model.db.migration

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.db.AppDatabase
import com.skyd.podaura.model.db.instance
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Migration28To29Test {
    @Test
    fun upgradesExistingDatabaseWithNotificationsEnabledAndPreservesFeedView() = runTest {
        val directory = Files.createTempDirectory("feed-notification-migration-test").toFile()
        val databaseFile = File(directory, "test.db")
        try {
            val schema = Json.parseToJsonElement(
                checkNotNull(javaClass.getResource("/database/AppDatabase-28.json")).readText()
            ).jsonObject.getValue("database").jsonObject
            BundledSQLiteDriver().open(databaseFile.absolutePath).use { connection ->
                for (entity in schema.getValue("entities").jsonArray) {
                    val table = entity.jsonObject
                    val tableName = table.getValue("tableName").jsonPrimitive.content
                    connection.execSQL(table.getValue("createSql").jsonPrimitive.content
                        .replace("\${TABLE_NAME}", tableName))
                    for (index in table["indices"]?.jsonArray.orEmpty()) {
                        connection.execSQL(index.jsonObject.getValue("createSql").jsonPrimitive.content
                            .replace("\${TABLE_NAME}", tableName))
                    }
                }
                for (view in schema.getValue("views").jsonArray) {
                    val definition = view.jsonObject
                    connection.execSQL(definition.getValue("createSql").jsonPrimitive.content
                        .replace("\${VIEW_NAME}", definition.getValue("viewName").jsonPrimitive.content))
                }
                for (query in schema.getValue("setupQueries").jsonArray) {
                    connection.execSQL(query.jsonPrimitive.content)
                }
                connection.execSQL("PRAGMA user_version = 28")
                connection.execSQL(
                    "INSERT INTO Feed (url, title, sortXmlArticlesOnUpdate, mute, orderPosition, filterMask) " +
                            "VALUES ('https://example.com/old', 'Existing feed', 0, 1, 42, 0)"
                )
                connection.execSQL("INSERT INTO ArticleNotificationRule (id, name, regex) VALUES (1, 'Original rule', '.*news.*')")
            }
            val database = AppDatabase.instance(
                Room.databaseBuilder<AppDatabase>(databaseFile.absolutePath)
                    .setDriver(BundledSQLiteDriver())
            )
            try {
                // Opening through Room validates the complete migrated schema, including views.
                val existing = database.feedDao().getFeedView("https://example.com/old")
                assertTrue(database.feedDao().observeNotificationsEnabled(existing.feed.url).first())
                assertTrue(existing.feed.mute)
                assertEquals("Existing feed", existing.feed.title)
                assertEquals(42.0, existing.feed.orderPosition)
                assertEquals(0, existing.articleCount)
                database.feedDao().setFeed(FeedBean(url = "https://example.com/new"))
                assertTrue(database.feedDao().observeNotificationsEnabled("https://example.com/new").first())
                val rules = database.articleNotificationRuleDao().getAllArticleNotificationRules().first()
                assertEquals(3, rules.size)
                val original = rules.single { it.id == 1 }
                assertEquals("Original rule", original.name)
                assertEquals(".*news.*", original.regex)
                assertTrue(original.feedUrls.isEmpty() && original.groupIds.isEmpty())
                assertTrue(!original.isManaged)
                val managed = rules.single { it.isManaged && it.feedUrls == listOf(existing.feed.url) }
                assertEquals(listOf(existing.feed.url), managed.feedUrls)
                assertTrue(managed.regex.isEmpty() && managed.isValid())
            } finally {
                database.close()
            }
        } finally {
            directory.deleteRecursively()
        }
    }
}
