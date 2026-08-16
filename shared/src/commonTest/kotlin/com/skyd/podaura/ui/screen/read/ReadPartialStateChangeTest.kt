package com.skyd.podaura.ui.screen.read

import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.article.ArticleWithEnclosureBean
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.bean.translation.ArticleTranslation
import com.skyd.podaura.model.bean.translation.TranslationError
import com.skyd.podaura.model.repository.fullcontent.FullContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
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

    @Test
    fun successfulTranslationSwitchesToTranslatedAndCanReturnToOriginal() {
        val state = stateWithFullContent()
        val translated = ReadPartialStateChange.TranslationResult.Success(
            contentSource = ReadContentSource.Feed,
            profileId = "profile",
            targetLanguage = "ZH",
            translation = ArticleTranslation(
                title = "Translated title",
                html = "<p>Translated</p>",
                detectedSourceLanguage = "EN",
                fromCache = false,
            ),
        ).reduce(state)

        assertIs<TranslationStatus.Success>(translated.translationState.status)
        assertEquals(TranslationDisplayMode.Translated, translated.translationState.displayMode)
        assertEquals("Translated title", translated.translationState.translatedTitle)

        val original = ReadPartialStateChange.TranslationDisplayModeChanged(
            TranslationDisplayMode.Original
        ).reduce(translated)
        assertEquals(TranslationDisplayMode.Original, original.translationState.displayMode)
    }

    @Test
    fun sourceSwitchClearsDisplayedTranslationButRetainsCacheSelection() {
        val translated = ReadPartialStateChange.TranslationResult.Success(
            contentSource = ReadContentSource.Feed,
            profileId = "profile",
            targetLanguage = "JA",
            translation = ArticleTranslation("Title", "<p>Text</p>", "EN", false),
        ).reduce(stateWithFullContent())

        val switched = ReadPartialStateChange.ContentSourceChanged(
            ReadContentSource.FullText
        ).reduce(translated)

        assertEquals(ReadContentSource.FullText, (switched.articleState as ArticleState.Success).contentSource)
        assertEquals("profile", switched.translationState.profileId)
        assertEquals("JA", switched.translationState.targetLanguage)
        assertEquals(TranslationStatus.Idle, switched.translationState.status)
        assertNull(switched.translationState.translatedHtml)
    }

    @Test
    fun translationForAnotherContentSourceCannotReplaceVisibleArticle() {
        val state = stateWithFullContent().copy(
            articleState = (stateWithFullContent().articleState as ArticleState.Success).copy(
                contentSource = ReadContentSource.FullText,
            )
        )
        val result = ReadPartialStateChange.TranslationResult.Success(
            contentSource = ReadContentSource.Feed,
            profileId = "profile",
            targetLanguage = "ZH",
            translation = ArticleTranslation("Wrong", "<p>Wrong</p>", "EN", false),
        ).reduce(state)

        assertEquals(state, result)
    }

    @Test
    fun failedAndCancelledTranslationAlwaysKeepOriginalMode() {
        val loading = ReadPartialStateChange.TranslationResult.Loading(
            ReadContentSource.Feed,
            "profile",
            "ZH",
        ).reduce(stateWithFullContent())
        assertEquals(TranslationDisplayMode.Original, loading.translationState.displayMode)

        val failed = ReadPartialStateChange.TranslationResult.Failed(
            ReadContentSource.Feed,
            "profile",
            "ZH",
            TranslationError.Authentication,
        ).reduce(loading)
        assertIs<TranslationStatus.Failed>(failed.translationState.status)
        assertEquals(TranslationDisplayMode.Original, failed.translationState.displayMode)

        val cancelled = ReadPartialStateChange.TranslationResult.Cancelled.reduce(loading)
        assertEquals(TranslationStatus.Idle, cancelled.translationState.status)
        assertEquals("profile", cancelled.translationState.profileId)
    }

    @Test
    fun cancellationDoesNotClearAnExistingSuccessfulTranslation() {
        val translated = ReadPartialStateChange.TranslationResult.Success(
            contentSource = ReadContentSource.Feed,
            profileId = "profile",
            targetLanguage = "ZH",
            translation = ArticleTranslation("Title", "<p>Text</p>", "EN", false),
        ).reduce(stateWithFullContent())

        val result = ReadPartialStateChange.TranslationResult.Cancelled.reduce(translated)

        assertEquals(translated, result)
    }

    @Test
    fun cacheMissForAnotherSourceDoesNotClearTheVisibleTranslation() {
        val fullTextState = stateWithFullContent().copy(
            articleState = (stateWithFullContent().articleState as ArticleState.Success).copy(
                contentSource = ReadContentSource.FullText,
            )
        )
        val translated = ReadPartialStateChange.TranslationResult.Success(
            contentSource = ReadContentSource.FullText,
            profileId = "profile",
            targetLanguage = "ZH",
            translation = ArticleTranslation("Title", "<p>Full text</p>", "EN", false),
        ).reduce(fullTextState)

        val result = ReadPartialStateChange.TranslationResult.CacheMiss(
            contentSource = ReadContentSource.Feed,
            profileId = "profile",
            targetLanguage = "ZH",
        ).reduce(translated)

        assertEquals(translated, result)
    }

    private fun stateWithFullContent() = ReadState(
        articleState = ArticleState.Success(
            article = article("article-1"),
            feedContent = "<p>Feed</p>",
            fullContent = FullContent("<p>Full</p>", "https://example.com/article"),
            contentSource = ReadContentSource.Feed,
        ),
        loadingDialog = false,
        fullContentLoading = false,
    )

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
