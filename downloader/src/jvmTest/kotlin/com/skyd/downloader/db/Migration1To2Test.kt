package com.skyd.downloader.db

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Migration1To2Test {
    @Test
    fun addsNullableMetadataWithoutChangingExistingDownloads() = runTest {
        val connection = BundledSQLiteDriver().open(":memory:")
        try {
            connection.execSQL(
                """
                CREATE TABLE Download (
                    id INTEGER NOT NULL PRIMARY KEY,
                    url TEXT NOT NULL,
                    path TEXT NOT NULL,
                    fileName TEXT NOT NULL
                )
                """.trimIndent()
            )
            connection.execSQL(
                "INSERT INTO Download (id, url, path, fileName) " +
                        "VALUES (1, 'https://example.com/episode.mp3', '/tmp', 'episode.mp3')"
            )

            Migration1To2().migrate(connection)

            val columns = buildList {
                connection.prepare("PRAGMA table_info('Download')").use { statement ->
                    while (statement.step()) add(statement.getText(1))
                }
            }
            assertTrue("metadata" in columns)

            connection.prepare("SELECT id, metadata FROM Download").use { statement ->
                assertTrue(statement.step())
                assertEquals(1L, statement.getLong(0))
                assertTrue(statement.isNull(1))
            }
        } finally {
            connection.close()
        }
    }
}
