package com.skyd.podaura.nano.capabilities.articles.model.repository

import com.skyd.podaura.api.model.bean.article.ArticleQuery
import com.skyd.podaura.api.model.bean.article.ArticleRecord
import com.skyd.podaura.api.model.bean.article.ArticleSearchScope
import com.skyd.podaura.api.model.repository.IArticleRepository
import com.skyd.podaura.nano.capabilities.articles.model.bean.ArticleRecommendation
import com.skyd.podaura.nano.capabilities.articles.model.bean.ArticleSearchError
import com.skyd.podaura.nano.capabilities.articles.model.bean.AssistantAnswer
import com.skyd.podaura.nano.capabilities.articles.model.bean.AssistantArticle
import com.skyd.podaura.nano.capabilities.articles.model.bean.SearchEvidence
import com.skyd.podaura.nano.core.AssistantAnswerKind
import com.skyd.podaura.nano.core.AssistantError
import com.skyd.podaura.nano.core.AssistantException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ArticleSearchResultValidatorTest {
    private class Catalog : IArticleRepository {
        val rows = mutableMapOf<String, ArticleRecord>()
        val queries = mutableListOf<Pair<ArticleSearchScope, ArticleQuery>>()
        override suspend fun query(scope: ArticleSearchScope, query: ArticleQuery): List<ArticleRecord> {
            queries += scope to query
            return requireNotNull(query.withinArticleIds).reversed().mapNotNull(rows::get)
        }
    }

    @Test
    fun usesLocalDataPreservesModelOrderAndDropsChangedRows() = runTest {
        val catalog = Catalog()
        listOf("a", "b").forEach { catalog.rows[it] = ArticleRecord(it, title = "Local $it", sourceTitle = "Feed", content = "Text") }
        val query = ArticleQuery(read = false, publishedFromMillis = 10)
        val scope: ArticleSearchScope = ArticleSearchScope.Restricted(articleIds = setOf("a", "b", "deleted"))
        val evidence = listOf("a", "b", "deleted").associateWith {
            SearchEvidence(AssistantArticle(it, "Old", "Feed", snippet = "Text"), query)
        }
        val answer = AssistantAnswer(AssistantAnswerKind.RESULTS, "Found", listOf(
            ArticleRecommendation("b", "B"), ArticleRecommendation("b", "Duplicate"),
            ArticleRecommendation("deleted", "Gone"), ArticleRecommendation("a", "A"),
        ))
        val result = ArticleSearchResultValidator(LocalArticleSearchRepository(catalog)).validate(answer, scope, evidence, 1)
        assertEquals(listOf("b", "a"), result.articles.map { it.article.articleId })
        assertEquals("Local b", result.articles.first().article.title)
        assertTrue(result.resultsChanged)
        assertEquals(listOf(scope to query.copy(withinArticleIds = listOf("b", "deleted", "a"))), catalog.queries)
    }

    @Test
    fun resultDraftsAreDiscardedWhileNoMatchAndRequestExplanationsKeepTheirMeaning() = runTest {
        val catalog = Catalog()
        catalog.rows["a"] = ArticleRecord("a", title = "Local article", sourceTitle = "Feed", content = "Text")
        val evidence = mapOf("a" to SearchEvidence(
            AssistantArticle("a", "Old title", "Feed", snippet = "Text"), ArticleQuery(),
        ))
        val validator = ArticleSearchResultValidator(LocalArticleSearchRepository(catalog))
        val misleading = "当前搜索已关闭，候选内容尚未核验。"
        for (text in listOf(misleading, "")) {
            val result = validator.validate(AssistantAnswer(AssistantAnswerKind.RESULTS, text,
                listOf(ArticleRecommendation("a", "Relevant article"))), ArticleSearchScope.All, evidence, 1)
            assertEquals("", result.text)
            assertEquals("Local article", result.articles.single().article.title)
        }
        for (candidates in listOf(emptyMap(), evidence)) {
            val result = validator.validate(AssistantAnswer(AssistantAnswerKind.NO_MATCH, misleading),
                ArticleSearchScope.All, candidates, 1)
            assertEquals(AssistantAnswerKind.NO_MATCH, result.kind)
            assertEquals(candidates.isNotEmpty(), result.hadCandidates)
            assertEquals("", result.text)
            assertTrue(result.articles.isEmpty())
        }
        for (kind in listOf(AssistantAnswerKind.CLARIFICATION, AssistantAnswerKind.UNSUPPORTED)) {
            val draft = if (kind == AssistantAnswerKind.CLARIFICATION) "Which topic?" else "I cannot delete articles."
            assertEquals(draft, validator.validate(AssistantAnswer(kind, draft),
                ArticleSearchScope.All, emptyMap(), 0).text)
            assertFailsWith<AssistantException> {
                validator.validate(AssistantAnswer(kind, ""), ArticleSearchScope.All, emptyMap(), 0)
            }
        }
    }

    @Test
    fun refusesInventedIdsAndNoMatchWithoutSuccessfulSearch() = runTest {
        val validator = ArticleSearchResultValidator(LocalArticleSearchRepository(Catalog()))
        val unknown = AssistantAnswer(AssistantAnswerKind.RESULTS, "Found", listOf(ArticleRecommendation("invented", "Reason")))
        assertEquals(AssistantError.INVALID_OUTPUT, assertFailsWith<AssistantException> {
            validator.validate(unknown, ArticleSearchScope.All, emptyMap(), 1)
        }.error)
        assertFailsWith<AssistantException> {
            validator.validate(AssistantAnswer(AssistantAnswerKind.NO_MATCH, "None"), ArticleSearchScope.All, emptyMap(), 0)
        }
        assertEquals(AssistantAnswerKind.CLARIFICATION, validator.validate(
            AssistantAnswer(AssistantAnswerKind.CLARIFICATION, "Which topic?"), ArticleSearchScope.All, emptyMap(), 0,
        ).kind)
    }

    @Test
    fun allDeletedIsAChangedResultNotSuccessfulSearchOrZeroMatch() = runTest {
        val evidence = mapOf("a" to SearchEvidence(AssistantArticle("a", "A", "Feed", snippet = ""), ArticleQuery()))
        assertEquals(ArticleSearchError.RESULTS_CHANGED, assertFailsWith<AssistantException> {
            ArticleSearchResultValidator(LocalArticleSearchRepository(Catalog())).validate(
                AssistantAnswer(AssistantAnswerKind.RESULTS, "Found", listOf(ArticleRecommendation("a", "Reason"))),
                ArticleSearchScope.All, evidence, 1,
            )
        }.error)
    }
}
