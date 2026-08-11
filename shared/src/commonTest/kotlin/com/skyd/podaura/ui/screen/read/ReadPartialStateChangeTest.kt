package com.skyd.podaura.ui.screen.read

import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.article.ArticleWithEnclosureBean
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.repository.fullcontent.FullContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReadPartialStateChangeTest {
    @Test
    fun articleRefreshPreservesFullContentForTheSameArticle() {
        val fullContent = FullContent("<p>Full text</p>", "https://example.com/article")
        val oldState = ReadState(
            articleState = ArticleState.Success(
                article = article("article-1", favorite = false),
                feedContent = "Feed text",
                fullContent = fullContent,
                contentSource = ReadContentSource.FullText,
            ),
            loadingDialog = false,
            fullContentLoading = false,
        )

        val result = ReadPartialStateChange.ArticleResult.Success(
            article = article("article-1", favorite = true),
            feedContent = "Updated feed text",
        ).reduce(oldState).articleState as ArticleState.Success

        assertEquals(fullContent, result.fullContent)
        assertEquals(ReadContentSource.FullText, result.contentSource)
        assertEquals(true, result.article.articleWithEnclosure.article.isFavorite)
        assertEquals("Updated feed text", result.feedContent)
    }

    @Test
    fun articleRefreshDoesNotCarryFullContentToAnotherArticle() {
        val oldState = ReadState(
            articleState = ArticleState.Success(
                article = article("article-1"),
                feedContent = "Feed text",
                fullContent = FullContent("<p>Full text</p>", "https://example.com/one"),
                contentSource = ReadContentSource.FullText,
            ),
            loadingDialog = false,
            fullContentLoading = false,
        )

        val result = ReadPartialStateChange.ArticleResult.Success(
            article = article("article-2"),
            feedContent = "Other feed text",
        ).reduce(oldState).articleState as ArticleState.Success

        assertNull(result.fullContent)
        assertEquals(ReadContentSource.Feed, result.contentSource)
    }

    private fun article(id: String, favorite: Boolean = false): ArticleWithFeed = ArticleWithFeed(
        articleWithEnclosure = ArticleWithEnclosureBean(
            article = ArticleBean(
                articleId = id,
                feedUrl = "https://example.com/feed",
                isFavorite = favorite,
            ),
            enclosures = emptyList(),
            categories = emptyList(),
            media = null,
        ),
        feed = FeedBean(url = "https://example.com/feed"),
    )
}
