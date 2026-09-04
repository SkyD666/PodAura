package com.skyd.podaura.model.repository.download

import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.article.ArticleWithEnclosureBean
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.model.bean.article.RssMediaBean
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.download.ArticleDownloadSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class DownloadRepositoryTest {
    private val source = ArticleDownloadSource(
        articleId = "article-id",
        feedUrl = "https://example.com/feed.xml",
    )
    private val entry = ArticleDownloadSourceEntry(downloadId = "7", source = source)

    @Test
    fun resolvesCurrentArticleAndFeedMetadata() {
        val feed = feed()
        val result = resolveArticleDownloadInfo(
            sources = listOf(entry),
            articles = listOf(article(feed)),
            feeds = listOf(feed),
        ).getValue("7")

        assertEquals("Current episode title", result.articleTitle)
        assertEquals("episode.jpg", result.episodeImage)
        assertEquals("article.jpg", result.articleImage)
        assertEquals(feed, result.feed)
    }

    @Test
    fun keepsArticleDownloadPresentationWhenArticleWasDeletedButFeedStillExists() {
        val feed = feed()
        val result = resolveArticleDownloadInfo(
            sources = listOf(entry),
            articles = emptyList(),
            feeds = listOf(feed),
        ).getValue("7")

        assertNull(result.articleTitle)
        assertNull(result.episodeImage)
        assertEquals(feed, result.feed)
    }

    @Test
    fun fallsBackToDirectPresentationWhenFeedWasDeleted() {
        val result = resolveArticleDownloadInfo(
            sources = listOf(entry),
            articles = emptyList(),
            feeds = emptyList(),
        )

        assertFalse("7" in result)
    }

    @Test
    fun mediaResolutionFallsBackToFilenameWhenMimeLookupFails() {
        val files = listOf(
            CompletedDownloadFileEntry(
                downloadId = "audio",
                path = "/missing",
                fileName = "episode.mp3",
            ),
            CompletedDownloadFileEntry(
                downloadId = "document",
                path = "/missing",
                fileName = "notes.pdf",
            ),
        )

        val result = resolvePlayableDownloadIds(files) { error("File is missing") }

        assertEquals(setOf("audio"), result)
    }

    @Test
    fun mediaResolutionHonorsExplicitNonMediaMimeType() {
        val file = CompletedDownloadFileEntry(
            downloadId = "document",
            path = "/downloads",
            fileName = "document.mp3",
        )

        val result = resolvePlayableDownloadIds(listOf(file)) { "application/pdf" }

        assertFalse("document" in result)
    }

    private fun feed() = FeedBean(
        url = source.feedUrl,
        title = "Feed title",
        icon = "feed.jpg",
    )

    private fun article(feed: FeedBean) = ArticleWithFeed(
        articleWithEnclosure = ArticleWithEnclosureBean(
            article = ArticleBean(
                articleId = source.articleId,
                feedUrl = source.feedUrl,
                title = "Current episode title",
                image = "article.jpg",
            ),
            enclosures = emptyList(),
            categories = emptyList(),
            media = RssMediaBean(
                articleId = source.articleId,
                image = "episode.jpg",
            ),
        ),
        feed = feed,
    )
}
