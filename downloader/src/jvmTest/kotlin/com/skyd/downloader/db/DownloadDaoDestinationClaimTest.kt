package com.skyd.downloader.db

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.skyd.downloader.Status
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class DownloadDaoDestinationClaimTest {
    @Test
    fun onlyOneConcurrentTaskCanClaimResolvedDestination() = runTest {
        val databaseDirectory = Files.createTempDirectory("download-destination-claim").toFile()
        val database = DownloadDatabase.instance(
            Room.databaseBuilder<DownloadDatabase>(
                name = File(databaseDirectory, "downloads.db").absolutePath,
            ).setDriver(BundledSQLiteDriver())
        )
        try {
            val dao = database.downloadDao()
            dao.insert(startedDownload(id = "first", attemptId = "attempt-first", fileName = "a"))
            dao.insert(startedDownload(id = "second", attemptId = "attempt-second", fileName = "b"))
            val start = CompletableDeferred<Unit>()

            val claims = listOf(
                "first" to "attempt-first",
                "second" to "attempt-second",
            ).map { (id, attemptId) ->
                async(Dispatchers.Default) {
                    start.await()
                    dao.claimResponseMetadata(
                        id = id,
                        attemptId = attemptId,
                        path = "/downloads",
                        fileName = "resolved.mp3",
                        eTag = "",
                        lastModified = "",
                        finalUrl = "https://example.com/resolved.mp3",
                        totalBytes = 100,
                        updatedTime = 2,
                    )
                }
            }

            start.complete(Unit)

            assertEquals(listOf(0, 1), claims.awaitAll().sorted())
            assertEquals(
                1,
                dao.getAllEntity().count {
                    it.path == "/downloads" && it.fileName == "resolved.mp3"
                },
            )
        } finally {
            database.close()
            databaseDirectory.deleteRecursively()
        }
    }

    private fun startedDownload(
        id: String,
        attemptId: String,
        fileName: String,
    ) = DownloadEntity(
        id = id,
        url = "https://example.com/$fileName",
        path = "/downloads",
        fileName = fileName,
        requestedFileName = "",
        fileNameResolved = false,
        status = Status.Started.name,
        attemptId = attemptId,
        timeQueued = 1,
        createTime = 1,
        updatedTime = 1,
    )
}
