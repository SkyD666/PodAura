package com.skyd.podaura.model.db.migration

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Migration27To28Test {
    @Test
    fun createsOnlyPersistentProfileStorageForTranslation() = runTest {
        val connection = BundledSQLiteDriver().open(":memory:")
        try {
            connection.execSQL("CREATE TABLE ExistingArticle (id TEXT PRIMARY KEY, title TEXT)")
            connection.execSQL("INSERT INTO ExistingArticle VALUES ('article', 'Original')")
            Migration27To28().migrate(connection)

            val tables = buildSet {
                connection.prepare(
                    "SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name"
                ).use { statement ->
                    while (statement.step()) add(statement.getText(0))
                }
            }
            assertTrue("TranslationProfile" in tables)
            assertFalse("ArticleTranslation" in tables)
            connection.prepare("SELECT title FROM ExistingArticle WHERE id = 'article'").use {
                assertTrue(it.step())
                assertEquals("Original", it.getText(0))
            }

            val columns = buildList {
                connection.prepare("PRAGMA table_info('TranslationProfile')").use { statement ->
                    while (statement.step()) add(statement.getText(1))
                }
            }
            assertEquals(
                listOf(
                    "id", "name", "providerType", "endpoint", "credentialId",
                    "customHeadersJson", "requestTimeoutMillis", "enabled", "isDefault",
                    "targetLanguage", "providerConfigJson", "createdAt", "updatedAt",
                ),
                columns,
            )
        } finally {
            connection.close()
        }
    }
}
