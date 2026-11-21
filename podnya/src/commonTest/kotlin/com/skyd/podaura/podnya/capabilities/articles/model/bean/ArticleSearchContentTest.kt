package com.skyd.podaura.podnya.capabilities.articles.model.bean

import com.skyd.podaura.podnya.capabilities.articles.model.ArticleHistoryCodec

import com.skyd.podaura.podnya.core.AssistantAnswerKind
import com.skyd.podaura.podnya.core.AssistantHistorySnapshot
import com.skyd.podaura.podnya.core.AssistantLimits
import com.skyd.podaura.podnya.core.CapabilityRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArticleSearchContentTest {
    @Test
    fun validatedCardsRoundTripThroughCapabilityHistoryWithoutUnverifiedProse() {
        val answer = ValidatedAssistantAnswer(AssistantAnswerKind.RESULTS, "", listOf(card("local-id")))
        val content = ArticleSearchContent.recommendations(answer)
        val snapshot = content.historySnapshot()!!
        assertEquals("articles", snapshot.capabilityId)
        assertEquals("RESULTS", snapshot.summary)
        assertEquals(answer.articles, ArticleSearchContent.historyArticles(snapshot))
        assertNull(ArticleSearchContent.unranked(listOf(card("candidate").article)).historySnapshot())
        assertTrue(ArticleSearchContent.historyArticles(snapshot.copy(capabilityId = "episodes")).isEmpty())
        assertTrue(ArticleSearchContent.historyArticles(AssistantHistorySnapshot("articles", "", "invalid")).isEmpty())
    }

    @Test
    fun storedArticlesAndSelectionsSurviveRetiredEpisodeFields() {
        val articles = """[{"article":{"articleId":"a","title":"Episode 12","sourceName":"Feed","snippet":"","episodeNumber":12},"reason":"Reason"}]"""
        val recommendations = CapabilityRecord(
            capabilityId = "articles",
            version = 1,
            payload = """{"kind":"RESULTS","articles":$articles}""",
            fallback = "",
        )
        val decoded = assertIs<ArticleSearchContent>(ArticleHistoryCodec.decodeContent(recommendations))
        assertEquals("Episode 12", decoded.answer?.articles?.single()?.article?.title)
        val oldSnapshot = AssistantHistorySnapshot(capabilityId = "articles", summary = "RESULTS", payload = articles)
        assertEquals("a", ArticleReferences.from(oldSnapshot)?.items?.single()?.id)

        val selection = recommendations.copy(
            version = 2,
            payload = """{"items":[{"id":"a","title":"Episode 12","episodeNumber":12,"displayedTitle":"Updated title"},{"id":"b","title":"Pilot"}],"selectedId":"a","selectedEpisodeNumber":12,"selectedEpisodePart":"上","pendingField":"DATE","needsSelection":true,"choiceIds":["a","b"]}""",
        )
        val state = assertIs<ArticleReferences>(ArticleHistoryCodec.decodeContent(selection))
        assertEquals(listOf("a", "b"), state.items.map { it.id })
        assertEquals("Updated title", state.items.first().displayedTitle)
        assertEquals("a", state.selectedId)
        assertEquals(ArticleField.DATE, state.pendingField)
        assertEquals(listOf("a", "b"), state.choiceIds)
        assertTrue(state.needsSelection)
        assertEquals(state, ArticleReferences.from(oldSnapshot.copy(payload = selection.payload)))
        val saved = assertNotNull(ArticleHistoryCodec.encodeContent(state))
        assertEquals(state, ArticleHistoryCodec.decodeContent(saved))
    }

    @Test
    fun oversizedCardsAreDroppedWholeAndRemainingIdsArePreserved() {
        val content = ArticleSearchContent.recommendations(ValidatedAssistantAnswer(
            AssistantAnswerKind.RESULTS, "", listOf(card("keep-id"), card("large-id", "喵".repeat(800_000))),
        ))
        assertTrue(content.retainedBytes <= AssistantLimits.CONTENT_BYTES)
        assertEquals(listOf("keep-id"), content.answer!!.articles.map { it.article.articleId })
        assertTrue(content.answer.resultsChanged)
        val candidates = ArticleSearchContent.unranked(listOf(card("keep-id").article, card("large-id", "x".repeat(AssistantLimits.TURN_BYTES)).article))
        assertEquals(listOf("keep-id"), candidates.candidates.map { it.articleId })
        assertTrue(candidates.retainedBytes <= AssistantLimits.CONTENT_BYTES)
    }

    @Test
    fun noMatchOutcomeAndCandidateFlagRemainCapabilityOwned() {
        val content = ArticleSearchContent.recommendations(ValidatedAssistantAnswer(
            AssistantAnswerKind.NO_MATCH, "", emptyList(), hadCandidates = true,
        ))
        assertEquals("NO_MATCH", content.historySnapshot()!!.summary)
        assertTrue(content.answer!!.hadCandidates)
        assertTrue(ArticleSearchContent.historyArticles(content.historySnapshot()).isEmpty())
    }

    private fun card(id: String, title: String = "Local article") = RecommendedArticle(
        AssistantArticle(id, title, "Source", snippet = "Local snippet"), "Reason",
    )
}
