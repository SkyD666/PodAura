package com.skyd.podaura.model.download

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ArticleDownloadSourceTest {
    @Test
    fun roundTripsStructuredDownloadMetadata() {
        val source = ArticleDownloadSource(
            articleId = "article-id",
            feedUrl = "https://example.com/feed.xml",
        )

        assertEquals(source, source.encode().decodeArticleDownloadSource())
    }

    @Test
    fun treatsMissingOrUnrecognizedMetadataAsDirectDownload() {
        assertNull(null.decodeArticleDownloadSource())
        assertNull("not-json".decodeArticleDownloadSource())
        assertNull("{\"unrelated\":true}".decodeArticleDownloadSource())
    }
}
