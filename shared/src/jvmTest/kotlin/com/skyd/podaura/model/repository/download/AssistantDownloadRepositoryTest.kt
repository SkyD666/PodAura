package com.skyd.podaura.model.repository.download

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.skyd.downloader.Downloader
import com.skyd.downloader.Status
import com.skyd.downloader.db.DownloadDatabase
import com.skyd.downloader.db.instance
import com.skyd.downloader.di.downloaderModule
import com.skyd.downloader.download.DownloadFailureCode
import com.skyd.podaura.api.model.bean.download.AttachmentMediaKind
import com.skyd.podaura.api.model.bean.download.AssistantDownloadScope
import com.skyd.podaura.api.model.bean.download.AssistantDownloadTarget
import com.skyd.podaura.api.model.repository.IAssistantDownloadRepository
import com.skyd.podaura.di.assistantDownloadHostModule
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.article.EnclosureBean
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.db.AppDatabase
import com.skyd.podaura.model.db.instance
import com.skyd.podaura.model.download.ArticleDownloadSource
import com.skyd.podaura.model.download.decodeArticleDownloadSource
import com.skyd.podaura.model.preference.data.medialib.MediaLibLocationPreference
import com.skyd.podaura.model.repository.media.MediaRepository
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.File
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AssistantDownloadRepositoryTest {
    @Test
    fun classifiesAttachmentsFromTheFileNameAndDeclaredMimeType() {
        fun kind(path: String, type: String?) = detectLocalMediaKind(
            EnclosureBean(articleId = "a", url = "https://example.test/$path", length = 0L, type = type)
        )
        assertEquals(AttachmentMediaKind.Audio, kind("episode%2EWMA?sig=preview.mp4", null))
        assertEquals(AttachmentMediaKind.Video, kind("clip.M4V#preview.mp3", "application/octet-stream"))
        assertEquals(AttachmentMediaKind.Audio, kind("clip.oga", null))
        assertEquals(AttachmentMediaKind.Video, kind("clip.ogv", null))
        assertEquals(AttachmentMediaKind.Other, kind("archive.mp3/", null))
        assertEquals(AttachmentMediaKind.Other, kind("document.mp3", "application/pdf"))
        assertEquals(AttachmentMediaKind.Other, kind("notes.m3u8?sig=1", "application/pdf"))
        assertEquals(AttachmentMediaKind.Playlist, kind("stream.m3u?sig=1", "application/octet-stream"))
        assertEquals(AttachmentMediaKind.Playlist, kind("stream.m3u8", "video/mp4"))
        assertEquals(AttachmentMediaKind.Playlist, kind("asset", "application/vnd.apple.mpegurl"))
    }

    @Test
    fun expandsLocalSubscriptionsAndDownloadsVerifiedFilesThroughApplicationQueue() = runBlocking {
        val temp = createTempDirectory("assistant-download").toFile()
        val media = temp.resolve("media").apply { mkdirs() }
        val articles = AppDatabase.instance(
            Room.databaseBuilder<AppDatabase>(temp.resolve("articles.db").path)
                .setDriver(BundledSQLiteDriver()),
        )
        val downloads = DownloadDatabase.instance(
            Room.databaseBuilder<DownloadDatabase>(temp.resolve("downloads.db").path)
                .setDriver(BundledSQLiteDriver()),
        )
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val sample = "assistant download test file"
        val fileGets = AtomicInteger()
        val pageGets = AtomicInteger()
        server.createContext("/") { exchange ->
            exchange.use {
                val uri = exchange.requestURI
                when {
                    uri.path == "/asset" && exchange.requestMethod == "HEAD" -> {
                        val status = if (uri.query == "signature=file") 404 else 403
                        exchange.sendResponseHeaders(status, -1)
                    }
                    uri.path == "/asset" && uri.query == "signature=file" -> {
                        if (fileGets.incrementAndGet() == 1) {
                            exchange.respondFile(type = "application/octet-stream", body = sample)
                        } else {
                            exchange.sendResponseHeaders(410, -1)
                        }
                    }
                    uri.path == "/asset" && uri.query == "signature=page" -> {
                        pageGets.incrementAndGet()
                        exchange.respondFile(type = "text/html", body = "<html><video></video></html>")
                    }
                    uri.path == "/asset" -> exchange.sendResponseHeaders(400, -1)
                    uri.path == "/watch" -> exchange.respondFile(type = "text/html", body = "<video>")
                    uri.path == "/misleading.mp3" && exchange.requestMethod == "HEAD" -> {
                        exchange.respondFile(type = "audio/mpeg", body = "")
                    }
                    uri.path == "/misleading.mp3" -> {
                        exchange.respondFile(
                            type = "application/octet-stream",
                            body = "<!--player--><!doctype html><video></video>",
                        )
                    }
                    else -> exchange.respondFile(type = "application/octet-stream", body = sample)
                }
            }
        }
        server.start()
        val baseUrl = "http://127.0.0.1:${server.address.port}"
        val prefs = preferencesOf(checkNotNull(MediaLibLocationPreference.key) to media.path)
        val koin = startKoin {
            modules(
                downloaderModule,
                assistantDownloadHostModule,
                module {
                    single { downloads }
                    single { downloads.downloadDao() }
                    single { articles.articleDao() }
                    single { articles.enclosureDao() }
                    single { articles.feedDao() }
                    single { articles.groupDao() }
                    single<DataStore<Preferences>> {
                        object : DataStore<Preferences> {
                            override val data = MutableStateFlow(prefs)
                            override suspend fun updateData(
                                transform: suspend (Preferences) -> Preferences,
                            ): Preferences = transform(data.value).also { data.value = it }
                        }
                    }
                    single {
                        MediaRepository(
                            json = Json,
                            feedDao = get(),
                            articleDao = get(),
                        )
                    }
                },
            )
        }.koin
        try {
            val repository = koin.get<IAssistantDownloadRepository>()
            val downloader = koin.get<Downloader>()
            val feedUrl = "$baseUrl/feed"
            val otherFeedUrl = "$baseUrl/other-feed"
            articles.feedDao().setFeed(FeedBean(url = feedUrl, title = "Podcast"))
            articles.feedDao().setFeed(FeedBean(url = otherFeedUrl, title = "Other subscription"))
            for (number in 1..134) {
                articles.articleDao().innerUpsertArticle(
                    ArticleBean(
                        articleId = "episode-$number",
                        feedUrl = feedUrl,
                        date = number.toLong(),
                    ),
                )
            }
            articles.articleDao().innerUpsertArticle(
                ArticleBean(articleId = "other", feedUrl = otherFeedUrl, date = 134),
            )
            val attachment = AssistantDownloadTarget.Attachment(
                articleId = "episode-134",
                url = "$baseUrl/episode.MP3?token=local",
            )
            articles.enclosureDao().upsert(
                listOf(
                    EnclosureBean(
                        articleId = attachment.articleId,
                        url = attachment.url,
                        length = 0L,
                        type = null,
                    ),
                    EnclosureBean(
                        articleId = attachment.articleId,
                        url = "$baseUrl/transcript.pdf",
                        length = 0L,
                        type = "application/pdf",
                    ),
                ),
            )

            assertEquals(134, repository.articlesInFeed(AssistantDownloadScope(feedUrl)).size)
            val episodes = repository.articlesInFeed(
                AssistantDownloadScope(feedUrl = feedUrl, publishedFromMillis = 1, publishedBeforeMillis = 133, limit = 3),
            )
            assertEquals(listOf("episode-132", "episode-131", "episode-130"), episodes.map { it.articleId })
            assertTrue(episodes.all { it.attachments.isEmpty() })
            val knownAttachment = repository.articlesByIds(listOf("missing-episode", attachment.articleId))
                .single().attachments.single { it.isMedia }
            assertEquals(attachment.url, knownAttachment.url)
            assertEquals(AttachmentMediaKind.Audio, knownAttachment.mediaKind)

            val direct = AssistantDownloadTarget.DirectLink("$baseUrl/asset?signature=file")
            val deferredPage = AssistantDownloadTarget.DirectLink("$baseUrl/asset?signature=page")
            val misleading = AssistantDownloadTarget.DirectLink("$baseUrl/misleading.mp3")
            assertTrue(repository.checkDirectLink(direct.url))
            assertTrue(repository.checkDirectLink(deferredPage.url))
            assertTrue(repository.checkDirectLink(misleading.url))
            assertFalse(repository.checkDirectLink("$baseUrl/watch"))
            assertEquals(0, fileGets.get())
            assertEquals(0, pageGets.get())
            val invalid = attachment.copy(url = "$baseUrl/invented.mp3")
            val submission = repository.submit(
                batchId = "chat:message",
                submissionId = "original",
                targets = listOf(attachment, attachment, invalid, direct, deferredPage, misleading),
            )
            assertEquals(setOf(attachment.key, direct.key, deferredPage.key, misleading.key), submission.queued.keys)
            assertEquals(setOf(invalid.key), submission.failed)
            val persisted = repository.receipt(batchId = "chat:message", submissionId = "original")
            assertEquals(submission, persisted)

            val rows = withTimeout(10_000) {
                downloader.observeDownloads().first { tasks ->
                    tasks.size == 4 && tasks.all { it.status in setOf(Status.Success.name, Status.Failed.name) }
                }
            }
            val rejectedIds = listOf(misleading, deferredPage).map { submission.queued.getValue(it.key) }
            for (row in rows.filter { it.id in rejectedIds }) {
                assertEquals(Status.Failed.name, row.status)
                assertEquals(DownloadFailureCode.Webpage.name, row.failureCode)
                assertFalse(File(row.path, row.fileName).exists())
            }
            assertEquals(1, fileGets.get())
            assertEquals(1, pageGets.get())
            for (row in rows.filterNot { it.id in rejectedIds }) {
                assertEquals(Status.Success.name, row.status, row.failureReason)
                assertEquals(sample, File(row.path, row.fileName).readText())
            }
            val articleRow = rows.single { it.id == submission.queued[attachment.key] }
            assertEquals(
                ArticleDownloadSource(articleId = attachment.articleId, feedUrl = feedUrl),
                articleRow.metadata.decodeArticleDownloadSource(),
            )
            assertNull(rows.single { it.id == submission.queued[direct.key] }.metadata)
            val destinationParent = assertNotNull(File(articleRow.path).parentFile)
            assertEquals(media.canonicalPath, destinationParent.canonicalPath)
        } finally {
            stopKoin()
            downloads.close()
            articles.close()
            server.stop(0)
            temp.deleteRecursively()
        }
    }

    private fun HttpExchange.respondFile(type: String, body: String) {
        responseHeaders.add("Content-Type", type)
        if (requestMethod == "HEAD") {
            sendResponseHeaders(200, -1)
        } else {
            val bytes = body.encodeToByteArray()
            sendResponseHeaders(200, bytes.size.toLong())
            responseBody.write(bytes)
        }
    }
}
