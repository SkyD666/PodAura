package com.skyd.downloader.db

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Migration2To3Test {
    @Test
    fun preservesLegacyDataAndPausesInterruptedTransfers() = runTest {
        val connection = BundledSQLiteDriver().open(":memory:")
        try {
            connection.execSQL(
                """
                CREATE TABLE Download (
                    id INTEGER NOT NULL PRIMARY KEY,
                    url TEXT NOT NULL,
                    path TEXT NOT NULL,
                    fileName TEXT NOT NULL,
                    timeQueued INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    totalBytes INTEGER NOT NULL,
                    downloadedBytes INTEGER NOT NULL,
                    speedInBytePerMs REAL NOT NULL,
                    eTag TEXT NOT NULL,
                    workerUuid TEXT NOT NULL,
                    createTime INTEGER NOT NULL,
                    userAction TEXT NOT NULL,
                    failureReason TEXT NOT NULL,
                    metadata TEXT
                )
                """.trimIndent()
            )
            connection.execSQL(
                """
                INSERT INTO Download VALUES (
                    42, 'https://example.com/episode.mp3?token=secret', '/downloads',
                    'episode.mp3', 1234, 'Downloading', 100, 25, 2.5,
                    '"etag"', 'obsolete-worker', 0, 'Start', '', 'source-json'
                )
                """.trimIndent()
            )

            Migration2To3().migrate(connection)

            val columns = buildList {
                connection.prepare("PRAGMA table_info('Download')").use { statement ->
                    while (statement.step()) add(statement.getText(1))
                }
            }
            assertTrue("requestedFileName" in columns)
            assertTrue("attemptId" in columns)
            assertFalse("legacyId" in columns)
            assertFalse("automatic" in columns)
            assertFalse("workerUuid" in columns)
            assertFalse("userAction" in columns)
            val indexes = buildList {
                connection.prepare("PRAGMA index_list('Download')").use { statement ->
                    while (statement.step()) add(statement.getText(1))
                }
            }
            assertTrue("index_Download_url_path_requestedFileName" in indexes)
            assertTrue("index_Download_path_fileName" in indexes)
            assertTrue("index_Download_status" in indexes)

            connection.prepare(
                "SELECT id, url, fileName, requestedFileName, status, " +
                        "downloadedBytes, eTag, createTime, metadata FROM Download"
            ).use { statement ->
                assertTrue(statement.step())
                assertEquals("legacy:42", statement.getText(0))
                assertEquals(
                    "https://example.com/episode.mp3?token=secret",
                    statement.getText(1),
                )
                assertEquals("episode.mp3", statement.getText(2))
                assertEquals("episode.mp3", statement.getText(3))
                assertEquals("Paused", statement.getText(4))
                assertEquals(25L, statement.getLong(5))
                assertEquals("\"etag\"", statement.getText(6))
                assertEquals(1234L, statement.getLong(7))
                assertEquals("source-json", statement.getText(8))
            }
        } finally {
            connection.close()
        }
    }
}
