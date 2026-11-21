package com.skyd.podaura.model.repository.download

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AssistantDownloadLinkTest {
    @Test
    fun usesHeadAndDefersGetOnlyExtensionlessFileLinksToTransfer() = runTest {
        val requests = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            assertEquals(HttpMethod.Head, request.method)
            val url = request.url.toString()
            requests += url
            when (url) {
                "https://example.com/signed?token=abc" -> respond(
                    content = "",
                    status = HttpStatusCode.Found,
                    headers = headersOf(HttpHeaders.Location, "/file"),
                )
                "https://example.com/file" -> respond(
                    content = "",
                    headers = headersOf(HttpHeaders.ContentType, "application/octet-stream"),
                )
                "https://example.com/asset?signature=abc" -> respond(
                    content = "",
                    status = HttpStatusCode.MethodNotAllowed,
                )
                else -> error("Unexpected request: $url")
            }
        }) { followRedirects = false }

        try {
            assertTrue(client.verifyAssistantDownloadLink("https://example.com/signed?token=abc"))
            assertTrue(client.verifyAssistantDownloadLink("https://example.com/asset?signature=abc"))
            assertEquals(
                expected = listOf(
                    "https://example.com/signed?token=abc",
                    "https://example.com/file",
                    "https://example.com/asset?signature=abc",
                ),
                actual = requests,
            )
        } finally {
            client.close()
        }
    }

    @Test
    fun recognizesHtmlPlaybackPagesAndHtmlFilesServedAsAttachments() = runTest {
        val client = HttpClient(MockEngine { request ->
            assertEquals(HttpMethod.Head, request.method)
            when (request.url.toString()) {
                "https://example.com/player.mp3" -> respond(
                    content = "",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "text/HTML; charset=utf-8"),
                )
                "https://example.com/player" -> respond(
                    content = "",
                    headers = headersOf(HttpHeaders.ContentType, "application/xhtml+xml"),
                )
                "https://example.com/player.php" -> respond("")
                "https://example.com/report" -> respond(
                    content = "",
                    status = HttpStatusCode.OK,
                    headers = headersOf(
                        HttpHeaders.ContentType to listOf("text/html"),
                        HttpHeaders.ContentDisposition to listOf("attachment; filename=report.html"),
                    ),
                )
                else -> error("Unexpected request")
            }
        }) { followRedirects = false }

        try {
            assertFalse(client.verifyAssistantDownloadLink("https://example.com/player.mp3"))
            assertFalse(client.verifyAssistantDownloadLink("https://example.com/player"))
            assertFalse(client.verifyAssistantDownloadLink("https://example.com/player.php"))
            assertTrue(client.verifyAssistantDownloadLink("https://example.com/report"))
        } finally {
            client.close()
        }
    }

    @Test
    fun onlyProbesHttpLinksAndSeparatesNetworkFailureFromPlaybackPages() = runTest {
        val client = HttpClient(MockEngine { request ->
            assertEquals(HttpMethod.Head, request.method)
            when (request.url.toString()) {
                "https://example.com/downgrade" -> respond(
                    content = "",
                    status = HttpStatusCode.TemporaryRedirect,
                    headers = headersOf(HttpHeaders.Location, "http://example.com/file"),
                )
                "https://example.com/unavailable" -> respond(
                    content = "",
                    status = HttpStatusCode.ServiceUnavailable,
                )
                else -> error("Unexpected request")
            }
        }) { followRedirects = false }

        try {
            assertFalse(client.verifyAssistantDownloadLink("file:///episode.mp3"))
            assertFalse(client.verifyAssistantDownloadLink("https://user@example.com/episode.mp3"))
            assertFalse(client.verifyAssistantDownloadLink("https://example.com/downgrade"))
            assertFailsWith<IOException> {
                client.verifyAssistantDownloadLink("https://example.com/unavailable")
            }
        } finally {
            client.close()
        }
    }
}
