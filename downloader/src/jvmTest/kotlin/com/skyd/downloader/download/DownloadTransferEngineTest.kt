package com.skyd.downloader.download

import com.skyd.downloader.db.DownloadEntity
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DownloadTransferEngineTest {
    @Test
    fun rangeIgnoredByServerSafelyReplacesPartialContent() = runTest {
        val directory = Files.createTempDirectory("downloader-test")
        directory.resolve("episode.mp3.part").writeText("old")
        var rangeHeader: String? = null
        val client = HttpClient(MockEngine { request ->
            rangeHeader = request.headers[HttpHeaders.Range]
            respond(
                content = "fresh",
                status = HttpStatusCode.OK,
                headers = headersOf(
                    HttpHeaders.ContentLength to listOf("5"),
                    HttpHeaders.ETag to listOf("\"v2\""),
                ),
            )
        }) { followRedirects = false }
        try {
            val result = DownloadTransferEngine(client).transfer(
                entity = entity(directory.toString(), eTag = "\"v1\""),
                onResponse = { "episode.mp3" },
                onProgress = {},
            )

            assertEquals("bytes=3-", rangeHeader)
            assertEquals(5, result.totalBytes)
            assertEquals("fresh", directory.resolve("episode.mp3").readText())
            assertFalse(directory.resolve("episode.mp3.part").exists())
        } finally {
            client.close()
        }
    }

    @Test
    fun invalidContentRangeRestartsWithoutAppending() = runTest {
        val directory = Files.createTempDirectory("downloader-test")
        directory.resolve("episode.mp3.part").writeText("abc")
        var requestCount = 0
        val client = HttpClient(MockEngine { request ->
            requestCount++
            if (request.headers[HttpHeaders.Range] != null) {
                respond(
                    content = "abc",
                    status = HttpStatusCode.PartialContent,
                    headers = headersOf(
                        HttpHeaders.ContentRange to listOf("bytes 0-2/6"),
                        HttpHeaders.ContentLength to listOf("3"),
                    ),
                )
            } else {
                respond(
                    content = "abcdef",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentLength, "6"),
                )
            }
        }) { followRedirects = false }
        try {
            DownloadTransferEngine(client).transfer(
                entity = entity(directory.toString(), eTag = "\"v1\""),
                onResponse = { "episode.mp3" },
                onProgress = {},
            )

            assertEquals(2, requestCount)
            assertEquals("abcdef", directory.resolve("episode.mp3").readText())
        } finally {
            client.close()
        }
    }

    @Test
    fun rejectsHttpsToHttpRedirect() = runTest {
        val directory = Files.createTempDirectory("downloader-test")
        val client = HttpClient(MockEngine {
            respond(
                content = "",
                status = HttpStatusCode.Found,
                headers = headersOf(HttpHeaders.Location, "http://example.com/file.mp3"),
            )
        }) { followRedirects = false }
        try {
            val failure = assertFailsWith<DownloadFailure> {
                DownloadTransferEngine(client).transfer(
                    entity = entity(directory.toString()),
                    onResponse = { "episode.mp3" },
                    onProgress = {},
                )
            }
            assertEquals(DownloadFailureCode.InsecureRedirect, failure.code)
        } finally {
            client.close()
        }
    }

    @Test
    fun directLinksValidateTheActualBodyBeforeWritingAndPreserveAcceptedBytes() = runTest {
        val directory = Files.createTempDirectory("downloader-direct-link-test")
        val contents = listOf(
            "plain login" to "text/html; charset=UTF-8",
            "\uFEFF <!-- site -->\n<!DOCTYPE HTML><video>" to "application/octet-stream",
            "binary data ".repeat(70) to "application/octet-stream",
            "<html>deliberate attachment</html>" to "text/html",
            "<html>ordinary attachment</html>" to "text/html",
        )
        val client = HttpClient(MockEngine { request ->
            val index = request.url.segments.last().substringBefore('.').toInt()
            val (body, mime) = contents[index]
            respond(
                content = body,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf(mime),
                    HttpHeaders.ContentDisposition to if (index == 3) {
                        listOf("attachment; filename=archive.html")
                    } else {
                        emptyList()
                    },
                ),
            )
        })
        try {
            val engine = DownloadTransferEngine(client)
            for ((index, content) in contents.withIndex()) {
                val name = "$index.mp3"
                val download = entity(directory.toString()).copy(
                    url = "https://example.com/$name",
                    fileName = name,
                    requireFileResponse = index < 4,
                    batchId = "direct".takeIf { index < 4 },
                )
                if (index < 2) {
                    val failure = assertFailsWith<DownloadFailure> {
                        engine.transfer(entity = download, onResponse = { name }, onProgress = {})
                    }
                    assertEquals(DownloadFailureCode.Webpage, failure.code)
                    assertFalse(failure.retryable)
                    assertFalse(directory.resolve(name).exists())
                    assertFalse(directory.resolve("$name.part").exists())
                } else {
                    engine.transfer(entity = download, onResponse = { name }, onProgress = {})
                    assertEquals(content.first, directory.resolve(name).readText())
                }
            }
        } finally {
            client.close()
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun batchTransferPreservesAFileThatAppearsAfterClaimingTheDestination() = runTest {
        val directory = Files.createTempDirectory("downloader-collision-test")
        val target = directory.resolve("episode.mp3")
        val client = HttpClient(MockEngine { respond("new bytes") })
        try {
            val task = entity(directory.toString()).copy(batchId = "batch")
            val failure = assertFailsWith<DownloadFailure> {
                DownloadTransferEngine(client).transfer(
                    entity = task,
                    files = BatchDownloadFiles(entity = task, savePublication = { _, _ -> }),
                    onResponse = { "episode.mp3" },
                    onProgress = {
                        if (it.downloadedBytes > 0) target.writeText("user file")
                    },
                )
            }
            assertEquals(DownloadFailureCode.Storage, failure.code)
            assertTrue(target.exists())
            assertEquals("user file", target.readText())
        } finally {
            client.close()
            directory.toFile().deleteRecursively()
        }
    }

    private fun entity(path: String, eTag: String = "") = DownloadEntity(
        id = "task-id",
        url = "https://example.com/episode.mp3?token=secret",
        path = path,
        fileName = "episode.mp3",
        requestedFileName = "episode.mp3",
        eTag = eTag,
        attemptId = "attempt-id",
    )
}
