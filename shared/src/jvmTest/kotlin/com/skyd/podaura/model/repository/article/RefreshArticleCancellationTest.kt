package com.skyd.podaura.model.repository.article

import androidx.paging.PagingConfig
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.skyd.downloader.download.DownloadConstraints
import com.skyd.podaura.model.bean.article.ArticleWithEnclosureBean
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.bean.feed.FeedWithArticleBean
import com.skyd.podaura.model.db.AppDatabase
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.db.dao.FeedDao
import com.skyd.podaura.model.db.instance
import com.skyd.podaura.model.download.ArticleDownloadSource
import com.skyd.podaura.model.download.DownloadInfoBean
import com.skyd.podaura.model.repository.download.IDownloadManager
import com.skyd.podaura.model.repository.feed.RssHelper
import com.skyd.podaura.util.favicon.FaviconExtractor
import com.skyd.podaura.util.favicon.extractor.BaseUrlIconTagExtractor
import com.skyd.podaura.util.favicon.extractor.HardCodedExtractor
import com.skyd.podaura.util.favicon.extractor.IconTagExtractor
import com.sun.net.httpserver.HttpServer
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.encodedPath
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.net.InetSocketAddress
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RefreshArticleCancellationTest {
    @Test
    fun cancellationPreservesSavedFeedsFinishesSavingAndLeavesOtherRefreshesRunning() = runBlocking {
        withTimeout(20_000) {
            val directory = Files.createTempDirectory("refresh-cancellation").toFile()
            val database = AppDatabase.instance(
                Room.databaseBuilder<AppDatabase>(directory.resolve("test.db").absolutePath)
                    .setDriver(BundledSQLiteDriver()),
            )
            val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
            server.createContext("/") { exchange ->
                val isFeed = exchange.requestURI.path.endsWith(".xml")
                val body = if (isFeed) {
                    "<rss><channel><title>Updated</title><link>https://example.com</link>" +
                            "<description>Feed</description></channel></rss>"
                } else "<html></html>"
                exchange.responseHeaders.add("Content-Type", if (isFeed) "application/rss+xml" else "text/html")
                val bytes = body.toByteArray()
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            server.start()
            val baseUrl = "http://127.0.0.1:${server.address.port}"
            val saved = CompletableDeferred<Unit>()
            val saving = CompletableDeferred<Unit>()
            val finishSaving = CompletableDeferred<Unit>()
            val slowStarted = CompletableDeferred<Unit>()
            val slowStopped = CompletableDeferred<Unit>()
            val backgroundStarted = CompletableDeferred<Unit>()
            val finishBackground = CompletableDeferred<Unit>()
            val blockedRequests = AtomicInteger()
            val stoppedRequests = AtomicInteger()
            val manualRequests = AtomicInteger()
            val savingUrl = AtomicReference<String>()
            val savedUrl = AtomicReference<String>()
            val clients = AtomicInteger()
            val closedClients = AtomicInteger()
            val blockSlowRequests = AtomicBoolean(true)
            val requests = createClientPlugin("ControlledRefreshRequests") {
                clients.incrementAndGet()
                onClose { closedClients.incrementAndGet() }
                onRequest { request, _ ->
                    if (request.url.encodedPath == "/background.xml") {
                        backgroundStarted.complete(Unit)
                        finishBackground.await()
                    } else if (blockSlowRequests.get()) {
                        // Assign roles by arrival: IO dispatch does not preserve URL order.
                        when (manualRequests.incrementAndGet()) {
                            1 -> savingUrl.set(request.url.buildString())
                            2 -> savedUrl.set(request.url.buildString())
                            else -> {
                                if (blockedRequests.incrementAndGet() == 4) slowStarted.complete(Unit)
                                try {
                                    awaitCancellation()
                                } finally {
                                    if (stoppedRequests.incrementAndGet() == 4) slowStopped.complete(Unit)
                                }
                            }
                        }
                    }
                }
            }
            val feedDao = object : FeedDao by database.feedDao() {
                override suspend fun updateFeedWithArticleIfExists(feedWithArticleBean: FeedWithArticleBean): Boolean {
                    if (feedWithArticleBean.feed.url == savingUrl.get()) {
                        saving.complete(Unit)
                        finishSaving.await()
                    }
                    val result = database.feedDao().updateFeedWithArticleIfExists(feedWithArticleBean)
                    if (feedWithArticleBean.feed.url == savedUrl.get()) saved.complete(Unit)
                    return result
                }
            }
            // Empty feeds need no article writes or notification delivery in this test.
            val articleDao = object : ArticleDao by database.articleDao() {
                override suspend fun insertListIfNotExist(articleWithEnclosureList: List<ArticleWithEnclosureBean>) {
                    assertTrue(articleWithEnclosureList.isEmpty())
                }
            }
            startKoin {
                modules(module {
                    single<ArticleDao> { articleDao }
                    single { HardCodedExtractor {} }
                    single { IconTagExtractor {} }
                    single { BaseUrlIconTagExtractor {} }
                    single { FaviconExtractor() }
                })
            }
            val downloads = object : IDownloadManager {
                override suspend fun getAllDownloadTasks(): List<DownloadInfoBean> = emptyList()
                override suspend fun download(
                    url: String, path: String, fileName: String?,
                    articleDownloadSource: ArticleDownloadSource?, constraints: DownloadConstraints,
                ): String = error("Unexpected download")
            }
            val repository = ArticleRepository(
                feedDao, articleDao, RssHelper { install(requests) }, PagingConfig(20),
                DownloadArticleProtectionResolver(downloads, database.enclosureDao()),
            )
            val manualUrls = List(8) { "$baseUrl/manual$it.xml" }
            val backgroundUrl = "$baseUrl/background.xml"
            try {
                (manualUrls + backgroundUrl).forEach { feedDao.setFeed(FeedBean(it, title = "Original")) }
                val background = launch { repository.refreshArticleList(listOf(backgroundUrl), full = false).collect() }
                val manual = launch { repository.refreshArticleList(manualUrls, full = false).collect() }
                try {
                    backgroundStarted.await()
                    saved.await()
                    saving.await()
                    slowStarted.await()
                    manual.cancel()
                    slowStopped.await()
                    assertFalse(manual.isCompleted)
                    assertTrue(background.isActive)
                    assertEquals(6, manualRequests.get()) // Two more requests are still queued.
                    assertEquals("Updated", feedDao.getFeed(savedUrl.get())?.title)
                    assertEquals("Original", feedDao.getFeed(savingUrl.get())?.title)

                    finishSaving.complete(Unit)
                    manual.join()
                    assertEquals(6, manualRequests.get())
                    assertEquals("Updated", feedDao.getFeed(savingUrl.get())?.title)
                    val unfinishedUrls = manualUrls - setOf(savedUrl.get(), savingUrl.get())
                    unfinishedUrls.forEach { assertEquals("Original", feedDao.getFeed(it)?.title) }
                    assertTrue(background.isActive)

                    blockSlowRequests.set(false)
                    repository.refreshArticleList(listOf(unfinishedUrls.first()), full = true).collect()
                    assertEquals("Updated", feedDao.getFeed(unfinishedUrls.first())?.title)
                    finishBackground.complete(Unit)
                    background.join()
                    assertEquals("Updated", feedDao.getFeed(backgroundUrl)?.title)
                    assertEquals(clients.get(), closedClients.get())
                } finally {
                    finishSaving.complete(Unit)
                    finishBackground.complete(Unit)
                    manual.cancelAndJoin()
                    background.cancelAndJoin()
                }
            } finally {
                stopKoin()
                server.stop(0)
                database.close()
                directory.deleteRecursively()
            }
        }
    }
}
