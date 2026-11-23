package com.skyd.downloader

import androidx.room3.Room
import androidx.room3.useWriterConnection
import androidx.room3.withWriteTransaction
import androidx.sqlite.SQLiteException
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.skyd.downloader.db.DownloadDatabase
import com.skyd.downloader.db.DownloadEntity
import com.skyd.downloader.db.instance
import com.skyd.downloader.download.DownloadExecutor
import com.skyd.downloader.download.DownloadFailure
import com.skyd.downloader.download.DownloadManager
import com.skyd.downloader.download.DownloadTransferEngine
import com.skyd.downloader.download.downloadTaskFiles
import io.github.vinceglb.filekit.path
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DownloadBatchTest {
    @Test
    fun overflowQueuesNothingAndFollowUpsSpendOnlyTheOriginalBatchesRemainingSlots() = runTest {
        withDatabase { database, directory ->
            val dao = database.downloadDao()
            val existing = DownloadEntity(
                id = "existing",
                url = item(directory, 130).url,
                path = directory.absolutePath,
                fileName = "episode-130.mp3",
                status = Status.Failed.name,
            )
            dao.insert(existing)

            val overflow = database.submit("overflow", (0..128).map { item(directory, it) })
            assertTrue(overflow.limitExceeded)
            assertEquals(129, overflow.matchedCount)
            assertEquals(128, overflow.remainingCount)
            assertEquals(listOf(existing), dao.getAllEntity())
            assertEquals(overflow, database.submit("overflow", listOf(item(directory, 0))))

            val first = database.submit(
                submissionId = "initial",
                items = (0..125).map { item(directory, it) } +
                        listOf(item(directory, 130), item(directory, 1).copy(key = "duplicate")),
                failedKeys = setOf("could not resolve directory"),
            )
            assertFalse(first.limitExceeded)
            assertEquals(126, first.queued.size)
            assertEquals(128, first.matchedCount)
            assertEquals(2, first.remainingCount)
            assertEquals(mapOf("130" to existing.id), first.existing)
            assertEquals(setOf("could not resolve directory"), first.failed)
            assertEquals(Status.Failed.name, dao.find(existing.id)?.status)

            val secondOverflow = database.submit("too-many-choices", (126..128).map { item(directory, it) })
            assertTrue(secondOverflow.limitExceeded)
            assertTrue(secondOverflow.queued.isEmpty())
            assertEquals(127, dao.getAllEntity().size)

            val choices = database.submit(
                submissionId = "choices",
                items = (126..127).map { item(directory, it) } + item(directory, 3),
            )
            assertEquals(2, choices.queued.size)
            assertEquals(first.queued["3"], choices.existing["3"])
            assertEquals(0, choices.remainingCount)
            assertEquals(choices, database.submit("choices", emptyList()))
            assertEquals(129, dao.getAllEntity().size)

            dao.remove(first.queued.getValue("1"))
            val deletedDuplicate = database.submit("same-target", listOf(item(directory, 1).copy(key = "alias")))
            assertEquals(0, deletedDuplicate.matchedCount)
            assertFalse(deletedDuplicate.limitExceeded)
            assertTrue(deletedDuplicate.queued.isEmpty())
            val exhausted = database.submit("exhausted", listOf(item(directory, 129)))
            assertTrue(exhausted.limitExceeded)
            assertEquals(0, exhausted.remainingCount)
            assertEquals(128, dao.getAllEntity().size)
        }
    }

    @Test
    fun concurrentSubmissionsSerializeQuotaAndReceiptWrites() = runTest {
        withDatabase { database, directory ->
            val start = CompletableDeferred<Unit>()
            val firstItems = (0..127).map { item(directory, it) }
            val tasks = listOf(
                "large" to firstItems,
                "large" to firstItems,
                "small" to listOf(item(directory, 128)),
            ).map { (submissionId, items) ->
                async(Dispatchers.Default) {
                    start.await()
                    database.submit(submissionId, items)
                }
            }
            start.complete(Unit)
            val (large, duplicate, small) = tasks.awaitAll()

            assertEquals(large, duplicate)
            assertNotEquals(large.limitExceeded, small.limitExceeded)
            assertEquals(large.queued.size + small.queued.size, database.downloadDao().getAllEntity().size)
            assertEquals(large, database.downloadBatchReceiptDao().find("batch", "large")?.decodeBatchReceipt())
            assertEquals(small, database.downloadBatchReceiptDao().find("batch", "small")?.decodeBatchReceipt())
        }
    }

    @Test
    fun failureToPersistTheReceiptRollsBackTheWholeSubmission() = runTest {
        withDatabase { database, directory ->
            database.useWriterConnection { connection ->
                connection.usePrepared(
                    "CREATE TRIGGER rejectReceipt BEFORE INSERT ON DownloadBatchReceipt " +
                            "BEGIN SELECT RAISE(ABORT, 'receipt write failed'); END"
                ) { it.step() }
            }
            val error = assertFailsWith<SQLiteException> {
                database.submit("failed", listOf(item(directory, 0)))
            }
            assertTrue(error.message.orEmpty().contains("receipt write failed"))
            assertTrue(database.downloadDao().getAllEntity().isEmpty())
            assertNull(database.downloadBatchReceiptDao().find("batch", "failed"))
        }
    }

    @Test
    fun manuallyAddingAFailedGuardedUrlUsesOrdinaryResponseHandling() = runTest {
        withDatabase { database, directory ->
            val html = "<html>an intentionally saved page</html>"
            val client = HttpClient(MockEngine {
                respond(content = html, headers = headersOf(HttpHeaders.ContentType, "text/html"))
            })
            val dao = database.downloadDao()
            startKoin {
                modules(module {
                    single { dao }
                    single { DownloadExecutor(downloadDao = dao, transferEngine = DownloadTransferEngine(client)) }
                })
            }
            try {
                val downloader = Downloader(database = database, downloadManager = DownloadManager())
                val candidate = item(directory, 1).copy(requireFileResponse = true)
                val receipt = downloader.enqueueBatch(batchId = "batch", submissionId = "guarded", items = listOf(candidate))
                val taskId = receipt.queued.getValue(candidate.key)
                withContext(Dispatchers.Default) {
                    withTimeout(10_000) {
                        downloader.observeDownloads().first { tasks -> tasks.single().status == Status.Failed.name }
                    }
                }
                assertEquals("Webpage", downloader.find(taskId)?.failureCode)
                assertEquals(taskId, downloader.download(url = candidate.url, path = candidate.path))
                val downloaded = withContext(Dispatchers.Default) {
                    withTimeout(10_000) {
                        downloader.observeDownloads().first { tasks ->
                            tasks.single().status == Status.Success.name && tasks.single().publicationDigest == null
                        }.single()
                    }
                }
                assertFalse(downloaded.requireFileResponse)
                assertEquals(html, File(downloaded.path, downloaded.fileName).readText())
            } finally {
                stopKoin()
                client.close()
            }
        }
    }

    @Test
    fun databaseMigrationAndStartupPreserveReceiptsAndRestoreCommittedDownloads() = runTest {
        val directory = Files.createTempDirectory("download-batch-test").toFile()
        val file = File(directory, "downloads.db")
        val cancelStarted = CompletableDeferred<Unit>()
        val client = HttpClient(MockEngine { request ->
            if (request.url.encodedPath == "/episode-4.mp3") {
                cancelStarted.complete(Unit)
                awaitCancellation()
            }
            if (request.url.encodedPath.startsWith("/signed/")) {
                assertEquals("private", request.url.parameters["token"])
                return@MockEngine respond(
                    content = "",
                    status = HttpStatusCode.Found,
                    headers = headersOf(HttpHeaders.Location, "/episode-2.mp3"),
                )
            }
            assertNull(request.url.parameters["token"])
            respond(
                content = "audio",
                headers = headersOf(
                    HttpHeaders.ContentDisposition to listOf("attachment; filename=kept.m4a"),
                    HttpHeaders.ContentLength to listOf("5"),
                ),
            )
        }) { followRedirects = false }
        try {
            val legacy = DownloadEntity(
                id = "legacy",
                url = "https://example.com/legacy.mp3",
                path = directory.absolutePath,
                fileName = "shared.mp3",
                status = Status.Queued.name,
                attemptId = "legacy-attempt",
            )
            withOpenDatabase(file) { database -> database.downloadDao().insert(legacy) }
            BundledSQLiteDriver().open(file.absolutePath).use { connection ->
                connection.execSQL("DROP TABLE DownloadBatchReceipt")
                connection.execSQL("ALTER TABLE Download DROP COLUMN batchId")
                connection.execSQL("ALTER TABLE Download DROP COLUMN requireFileResponse")
                connection.execSQL("ALTER TABLE Download DROP COLUMN publicationDigest")
                connection.execSQL("PRAGMA user_version = 3")
            }
            val kept = File(directory, "kept.m4a").apply { writeText("original retained audio") }
            val provisionalConflict = File(directory, "episode-1.mp3").apply { writeText("original retained episode") }
            val keptTemporaries = listOf("episode-2.mp3", "episode-4.mp3")
                .flatMap { name -> listOf("$name.part", "$name.temp") }
                .map { name -> File(directory, name).apply { writeText(name) } }
            val committed = withOpenDatabase(file) { migrated ->
                assertEquals(legacy, migrated.downloadDao().find(legacy.id))
                val receipt = migrated.submit(
                    submissionId = "interrupted-before-scheduling",
                    items = listOf(item(directory, 1).copy(requireFileResponse = true)),
                )
                val queued = requireNotNull(migrated.downloadDao().find(receipt.queued.getValue("1")))
                assertNotEquals(provisionalConflict.name, queued.fileName)
                assertFalse(queued.fileNameResolved)
                assertTrue(queued.requireFileResponse)
                receipt
            }

            withOpenDatabase(file) { restarted ->
                val dao = restarted.downloadDao()
                startKoin {
                    modules(module {
                        single { dao }
                        single { DownloadExecutor(downloadDao = dao, transferEngine = DownloadTransferEngine(client)) }
                    })
                }
                try {
                    val downloader = Downloader(database = restarted, downloadManager = DownloadManager())
                    val restoredId = committed.queued.getValue("1")
                    assertEquals(committed, downloader.getBatchReceipt("batch", "interrupted-before-scheduling"))
                    assertEquals(Status.Queued.name, downloader.find(restoredId)?.status)
                    downloader.initialize()
                    val cancelled = async(start = CoroutineStart.UNDISPATCHED) {
                        currentCoroutineContext().cancel()
                        downloader.enqueueBatch(
                            batchId = "batch",
                            submissionId = "cancelled-before-submit",
                            items = listOf(item(directory, 4)),
                        )
                    }
                    assertFailsWith<CancellationException> { cancelled.await() }
                    assertNull(downloader.getBatchReceipt("batch", "cancelled-before-submit"))
                    val cancelledTransfer = downloader.enqueueBatch(
                        batchId = "batch",
                        submissionId = "cancel-transfer",
                        items = listOf(item(directory, 4)),
                    )
                    val cancelledId = cancelledTransfer.queued.getValue("4")
                    withContext(Dispatchers.Default) { withTimeout(10_000) { cancelStarted.await() } }
                    assertTrue(keptTemporaries.all { it.readText() == it.name })
                    withContext(Dispatchers.Default) { withTimeout(10_000) { downloader.cancel(cancelledId) } }
                    assertEquals(Status.Cancelled.name, downloader.find(cancelledId)?.status)
                    assertTrue(keptTemporaries.all { it.readText() == it.name })
                    val next = downloader.enqueueBatch(
                        batchId = "batch",
                        submissionId = "normal-entry",
                        items = listOf(
                            item(directory, 2).copy(url = "https://example.com/signed/episode-2.mp3?token=private"),
                            item(directory, 3).copy(url = "https://example.com/alt/episode-2.mp3"),
                        ),
                    )
                    val completed = withContext(Dispatchers.Default) {
                        withTimeout(10_000) {
                            downloader.observeDownloads().first { downloads ->
                                downloads.count { it.status == Status.Success.name } == 3
                            }
                        }
                    }
                    val successes = completed.filter { it.status == Status.Success.name }
                    assertEquals(setOf(restoredId) + next.queued.values, successes.map { it.id }.toSet())
                    assertEquals(3, successes.map { it.fileName }.distinct().size)
                    assertTrue(successes.all { it.fileName.startsWith("kept-") && it.fileName.endsWith(".m4a") })
                    assertTrue(successes.all { File(it.path, it.fileName).readText() == "audio" })
                    assertEquals("original retained audio", kept.readText())
                    assertEquals("original retained episode", provisionalConflict.readText())
                    assertTrue(keptTemporaries.all { it.readText() == it.name })
                    assertEquals(Status.Paused.name, downloader.find(legacy.id)?.status)
                    assertEquals(next, downloader.getBatchReceipt("batch", "normal-entry"))
                } finally {
                    stopKoin()
                }
            }
        } finally {
            client.close()
            directory.deleteRecursively()
        }
    }

    @Test
    fun startupRecoversAfterPublishingWithoutDownloadingOrCreatingAnotherFile() = runTest {
        val directory = Files.createTempDirectory("download-publication-test").toFile()
        val file = File(directory, "downloads.db")
        val requests = AtomicInteger()
        val client = HttpClient(MockEngine {
            requests.incrementAndGet()
            respond("unexpected network request")
        })
        try {
            val waiting = withOpenDatabase(file) { database ->
                val dao = database.downloadDao()
                val receipt = database.submit("publication-crash", (1..4).map { item(directory, it) })
                receipt.queued.mapValues { (key, id) ->
                    val entity = requireNotNull(dao.find(id))
                    assertEquals(1, dao.markStarted(id = id, attemptId = entity.attemptId, updatedTime = 2))
                    assertEquals(1, dao.claimResponseMetadata(
                        id = id,
                        attemptId = entity.attemptId,
                        path = entity.path,
                        fileName = entity.fileName,
                        eTag = "",
                        lastModified = "",
                        finalUrl = entity.url,
                        totalBytes = 5,
                        updatedTime = 3,
                    ))
                    val files = downloadTaskFiles(entity = entity, dao = dao)
                    val part = files.partial(entity.fileName)
                    File(part.path).writeText("audio")
                    if (key == "1" || key == "4") {
                        files.publish(fileName = entity.fileName, totalBytes = 5)
                    } else {
                        File(directory, entity.fileName).writeText("audio")
                        assertFailsWith<DownloadFailure> { files.publish(fileName = entity.fileName, totalBytes = 5) }
                    }
                    if (key == "3") {
                        dao.markFailed(
                            id = id,
                            attemptId = entity.attemptId,
                            reason = "Destination conflict",
                            code = "DestinationConflict",
                            updatedTime = 4,
                        )
                    }
                    if (key == "4") {
                        assertEquals(1, dao.markSuccess(id = id, attemptId = entity.attemptId, totalBytes = 5, updatedTime = 4))
                    }
                    assertTrue(File(part.path).exists())
                    entity
                }
            }

            withOpenDatabase(file) { database ->
                val dao = database.downloadDao()
                startKoin {
                    modules(module {
                        single { dao }
                        single { DownloadExecutor(downloadDao = dao, transferEngine = DownloadTransferEngine(client)) }
                    })
                }
                try {
                    val downloader = Downloader(database = database, downloadManager = DownloadManager())
                    val failed = waiting.getValue("3")
                    downloader.delete(id = failed.id, deleteFile = true)
                    assertEquals("audio", File(directory, failed.fileName).readText())
                    val failedPart = downloadTaskFiles(entity = failed, dao = dao).partial(failed.fileName)
                    assertFalse(File(failedPart.path).exists())
                    downloader.initialize()
                    val restored = withContext(Dispatchers.Default) {
                        withTimeout(10_000) {
                            downloader.observeDownloads().first { downloads ->
                                downloads.size == 3 && downloads.all {
                                    it.status == Status.Success.name && it.publicationDigest == null
                                }
                            }
                        }
                    }
                    val alreadyPublished = restored.first { it.id == waiting.getValue("1").id }
                    val newlyPublished = restored.first { it.id == waiting.getValue("2").id }
                    assertEquals("episode-1.mp3", alreadyPublished.fileName)
                    assertTrue(newlyPublished.fileName.startsWith("episode-2-") && newlyPublished.fileName.endsWith(".mp3"))
                    assertEquals("audio", File(directory, "episode-2.mp3").readText())
                    assertEquals(0, requests.get())
                    assertEquals(5, directory.listFiles().orEmpty().count { it.extension == "mp3" })
                    for (entity in restored) {
                        assertEquals("audio", File(directory, entity.fileName).readText())
                        assertEquals(5L, entity.totalBytes)
                        assertNull(entity.publicationDigest)
                        val part = downloadTaskFiles(entity = entity, dao = dao).partial(entity.fileName)
                        assertFalse(File(part.path).exists())
                    }
                } finally {
                    stopKoin()
                }
            }
        } finally {
            client.close()
            directory.deleteRecursively()
        }
    }

    private suspend fun withDatabase(block: suspend (DownloadDatabase, File) -> Unit) {
        val directory = Files.createTempDirectory("download-batch-test").toFile()
        try {
            withOpenDatabase(File(directory, "downloads.db")) { database -> block(database, directory) }
        } finally {
            directory.deleteRecursively()
        }
    }

    private suspend fun <T> withOpenDatabase(file: File, block: suspend (DownloadDatabase) -> T): T {
        val database = DownloadDatabase.instance(
            builder = Room.databaseBuilder<DownloadDatabase>(name = file.absolutePath)
                .setDriver(BundledSQLiteDriver()),
        )
        try {
            return block(database)
        } finally {
            database.close()
        }
    }

    private fun item(directory: File, number: Int) = DownloadBatchItem(
        key = number.toString(),
        url = "https://example.com/episode-$number.mp3",
        path = directory.absolutePath,
    )

    private suspend fun DownloadDatabase.submit(
        submissionId: String,
        items: List<DownloadBatchItem>,
        failedKeys: Set<String> = emptySet(),
    ): DownloadBatchReceipt = withWriteTransaction {
        downloadDao().submitBatch(
            receipts = downloadBatchReceiptDao(),
            batchId = "batch",
            submissionId = submissionId,
            items = items,
            failedKeys = failedKeys,
            now = 1,
        ).receipt
    }
}
