package com.skyd.podaura.model.repository.article

import com.skyd.downloader.Status
import com.skyd.downloader.download.DownloadConstraints
import com.skyd.podaura.model.bean.article.EnclosureBean
import com.skyd.podaura.model.db.dao.EnclosureDao
import com.skyd.podaura.model.download.ArticleDownloadSource
import com.skyd.podaura.model.download.DownloadInfoBean
import com.skyd.podaura.model.repository.download.IDownloadManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DownloadArticleProtectionResolverTest {
    @Test
    fun protectsExplicitArticleDownloadsInEveryStoredStatus() = runTest {
        val tasks = Status.entries.mapIndexed { index, status ->
            task(
                id = index.toString(),
                status = status,
                source = ArticleDownloadSource(
                    articleId = "article-$status",
                    feedUrl = "https://example.com/feed.xml",
                ),
            )
        }
        val downloadManager = FakeDownloadManager(tasks)
        val enclosureDao = FakeEnclosureDao(emptyMap())

        val result = DownloadArticleProtectionResolver(downloadManager, enclosureDao)
            .getProtectedArticleIds(enabled = true)

        assertEquals(tasks.map { it.articleDownloadSource!!.articleId }.toSet(), result.toSet())
        assertEquals(emptyList(), enclosureDao.requestedUrls)
    }

    @Test
    fun usesExactMetadataBeforeLegacyUrlFallbackAndProtectsDuplicateUrls() = runTest {
        val explicitUrl = "https://example.com/explicit.mp3"
        val legacyUrl = "https://example.com/legacy.mp3"
        val downloadManager = FakeDownloadManager(
            listOf(
                task(
                    id = "1",
                    url = explicitUrl,
                    source = ArticleDownloadSource(
                        articleId = "explicit-article",
                        feedUrl = "https://example.com/feed.xml",
                    ),
                ),
                task(id = "2", url = legacyUrl),
                task(id = "3", url = legacyUrl, status = Status.Success),
            )
        )
        val enclosureDao = FakeEnclosureDao(
            mapOf(
                explicitUrl to listOf("wrong-url-match"),
                legacyUrl to listOf("legacy-article-1", "legacy-article-2"),
            )
        )

        val result = DownloadArticleProtectionResolver(downloadManager, enclosureDao)
            .getProtectedArticleIds(enabled = true)

        assertEquals(
            setOf("explicit-article", "legacy-article-1", "legacy-article-2"),
            result.toSet(),
        )
        assertEquals(listOf(listOf(legacyUrl)), enclosureDao.requestedUrls)
    }

    @Test
    fun disabledProtectionDoesNotReadDownloadOrArticleDatabases() = runTest {
        val downloadManager = FakeDownloadManager(
            tasks = listOf(task(id = "1")),
            failOnRead = true,
        )
        val enclosureDao = FakeEnclosureDao(emptyMap())

        val result = DownloadArticleProtectionResolver(downloadManager, enclosureDao)
            .getProtectedArticleIds(enabled = false)

        assertEquals(emptyList(), result)
        assertEquals(0, downloadManager.readCount)
        assertEquals(emptyList(), enclosureDao.requestedUrls)
    }

    private fun task(
        id: String,
        url: String = "https://example.com/$id.mp3",
        status: Status = Status.Downloading,
        source: ArticleDownloadSource? = null,
    ) = DownloadInfoBean(
        id = id,
        url = url,
        path = "/downloads",
        fileName = "$id.mp3",
        status = status,
        totalBytes = 100,
        downloadedBytes = 50,
        speedInBytePerMs = 1f,
        createTime = id.toLong(),
        failureReason = "",
        articleDownloadSource = source,
    )

    private class FakeDownloadManager(
        private val tasks: List<DownloadInfoBean>,
        private val failOnRead: Boolean = false,
    ) : IDownloadManager {
        var readCount = 0

        override suspend fun download(
            url: String,
            path: String,
            fileName: String?,
            articleDownloadSource: ArticleDownloadSource?,
            constraints: DownloadConstraints,
        ): String = "0"

        override suspend fun getAllDownloadTasks(): List<DownloadInfoBean> {
            readCount++
            check(!failOnRead)
            return tasks
        }
    }

    private class FakeEnclosureDao(
        private val articleIdsByUrl: Map<String, List<String>>,
    ) : EnclosureDao {
        val requestedUrls = mutableListOf<List<String>>()

        override suspend fun queryEnclosureByLink(
            articleId: String,
            url: String?,
        ): EnclosureBean? = null

        override suspend fun innerUpdateEnclosure(enclosureBean: EnclosureBean) = Unit

        override suspend fun upsert(enclosureBeanList: List<EnclosureBean>) = Unit

        override suspend fun deleteEnclosure(enclosureBean: EnclosureBean): Int = 0

        override suspend fun deleteEnclosure(articleId: String): Int = 0

        override fun getEnclosureList(articleId: String): Flow<List<EnclosureBean>> =
            flowOf(emptyList())

        override suspend fun getMediaArticleId(path: String): String? = null

        override suspend fun getArticleIdsByUrls(urls: List<String>): List<String> {
            requestedUrls += urls
            return urls.flatMap { articleIdsByUrl[it].orEmpty() }
        }
    }
}
