package com.skyd.podaura.podnya.capabilities.articles.model.repository

import com.skyd.podaura.api.model.repository.ArticleSearchFieldsDisabledException
import com.skyd.podaura.api.model.repository.IArticleRepository
import com.skyd.podaura.api.model.bean.article.ArticleRecord
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchError
import com.skyd.podaura.api.model.bean.article.ArticleQuery
import com.skyd.podaura.api.model.bean.article.ArticleSearchScope
import com.skyd.podaura.podnya.core.AssistantException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalArticleSearchRepositoryTest {
    @Test
    fun historyOpenChecksOnlyTheSelectedLocalArticleAndReportsDeletion() = runTest {
        var exists = true
        val catalog = RecordingCatalog { query ->
            assertEquals(listOf("saved-id"), query.withinArticleIds)
            assertTrue(query.keywords.isEmpty())
            if (exists) listOf(ArticleRecord("saved-id")) else emptyList()
        }
        val source = LocalArticleSearchRepository(catalog)
        assertTrue(source.exists("saved-id"))
        exists = false
        assertEquals(false, source.exists("saved-id"))
    }

    @Test
    fun moduleOwnsHtmlFormattingSnippetWindowDateAndMoreResults() = runTest {
        val catalog = RecordingCatalog {
            listOf(
                ArticleRecord("a", title = "<b>Title</b>" + "t".repeat(300),
                    sourceTitle = "Unused feed title", sourceNickname = "<i>Nickname</i>" + "n".repeat(200),
                    dateMillis = 0, content = "<p>" + "x".repeat(200) + "AGENT" + "y".repeat(600) + "</p>"),
                ArticleRecord("b"),
            )
        }
        val query = ArticleQuery(keywords = listOf("agent"), limit = 1)
        val scope: ArticleSearchScope = ArticleSearchScope.Restricted(feedUrls = setOf("feed"))
        val page = LocalArticleSearchRepository(catalog).search(scope, query)
        assertEquals(listOf(scope to query.copy(limit = query.limit + 1)), catalog.calls)
        assertEquals(1, page.items.size)
        assertTrue(page.hasMore)
        val item = page.items.single()
        assertEquals("a", item.articleId)
        assertEquals("Title" + "t".repeat(195), item.title)
        assertEquals("Nickname" + "n".repeat(92), item.sourceName)
        assertEquals(0L, item.dateMillis)
        assertEquals("x".repeat(80) + "AGENT" + "y".repeat(315), item.snippet)
    }

    @Test
    fun parsingIsBoundedBeforeHtmlExtractionAndEmptyContentFallsBackToDescription() = runTest {
        val catalog = RecordingCatalog {
            listOf(
                ArticleRecord("bounded", title = "<script>" + "t".repeat(4096) + "</script>hidden title",
                    sourceTitle = "<script>" + "n".repeat(4096) + "</script>hidden source",
                    content = "x".repeat(64 * 1024) + "outside parsing budget"),
                ArticleRecord("fallback", content = "<script>ignored</script>",
                    description = "<p>Description &amp; fallback</p>", sourceTitle = "<b>Feed</b>"),
            )
        }
        val page = LocalArticleSearchRepository(catalog).search(ArticleSearchScope.All,
            ArticleQuery(keywords = listOf("outside parsing budget")))
        assertFalse(page.hasMore)
        assertEquals("", page.items.first().title)
        assertEquals("", page.items.first().sourceName)
        assertEquals("x".repeat(400), page.items.first().snippet)
        assertEquals("Description & fallback", page.items.last().snippet)
        assertEquals("Feed", page.items.last().sourceName)
        assertEquals(null, page.items.last().dateMillis)
    }

    @Test
    fun revalidationKeepsScopeAndHardFiltersButDoesNotRerunKeywordSearch() = runTest {
        val catalog = RecordingCatalog { query ->
            query.withinArticleIds.orEmpty().map { ArticleRecord(it, title = "Current title without the old keyword") } +
                ArticleRecord("unrequested")
        }
        val source = LocalArticleSearchRepository(catalog)
        val scope = ArticleSearchScope.Restricted(articleIds = (1..41).map { "a$it" }.toSet())
        val query = ArticleQuery(keywords = listOf("previous title"), publishedFromMillis = 100,
            publishedBeforeMillis = 300, read = false, favorite = true,
            withinArticleIds = scope.articleIds.toList(), excludeArticleIds = listOf("excluded"), limit = 5)
        val rows = source.resolve(scope, scope.articleIds.toList() + "a1" + "outside", query)
        assertEquals(scope.articleIds.toList(), rows.map { it.articleId })
        assertEquals(listOf(20, 20, 1), catalog.calls.map { it.second.withinArticleIds!!.size })
        catalog.calls.forEach { (actualScope, actualQuery) ->
            assertEquals(scope, actualScope)
            assertEquals(query.copy(keywords = emptyList(), withinArticleIds = actualQuery.withinArticleIds, limit = 20), actualQuery)
        }
        assertTrue(rows.all { it.title == "Current title without the old keyword" })
        assertTrue(source.resolve(scope, listOf("outside"), query).isEmpty())
        assertEquals(3, catalog.calls.size)
    }

    @Test
    fun queryBudgetsAreCheckedInModuleBeforeInvokingCatalog() = runTest {
        val catalog = RecordingCatalog { emptyList() }
        val source = LocalArticleSearchRepository(catalog)
        listOf(
            ArticleQuery(limit = 21),
            ArticleQuery(keywords = List(7) { "term" }),
            ArticleQuery(withinArticleIds = List(61) { "id" }),
            ArticleQuery(excludeArticleIds = List(61) { "id" }),
            ArticleQuery(publishedFromMillis = 300, publishedBeforeMillis = 100),
        ).forEach { query ->
            assertEquals(ArticleSearchError.LOCAL_SEARCH, assertFailsWith<AssistantException> {
                source.search(ArticleSearchScope.All, query)
            }.error)
        }
        assertEquals(ArticleSearchError.LOCAL_SEARCH, assertFailsWith<AssistantException> {
            source.resolve(ArticleSearchScope.All, List(61) { "id" }, ArticleQuery())
        }.error)
        assertTrue(catalog.calls.isEmpty())
    }

    @Test
    fun moduleMapsStorageFailuresPreservesDisabledFieldsAndRethrowsCancellation() = runTest {
        val unexpected = LocalArticleSearchRepository(RecordingCatalog { error("private storage detail") })
        val failure = assertFailsWith<AssistantException> {
            unexpected.search(ArticleSearchScope.All, ArticleQuery())
        }
        assertEquals(ArticleSearchError.LOCAL_SEARCH, failure.error)
        assertFalse(failure.message.orEmpty().contains("private storage detail"))
        val disabled = LocalArticleSearchRepository(RecordingCatalog {
            throw ArticleSearchFieldsDisabledException()
        })
        assertEquals(ArticleSearchError.SEARCH_FIELDS_DISABLED, assertFailsWith<AssistantException> {
            disabled.search(ArticleSearchScope.All, ArticleQuery(keywords = listOf("term")))
        }.error)
        val cancelled = LocalArticleSearchRepository(RecordingCatalog { throw CancellationException("stopped") })
        assertFailsWith<CancellationException> { cancelled.search(ArticleSearchScope.All, ArticleQuery()) }
    }
}

private class RecordingCatalog(private val records: suspend (ArticleQuery) -> List<ArticleRecord>) : IArticleRepository {
    val calls = mutableListOf<Pair<ArticleSearchScope, ArticleQuery>>()
    override suspend fun query(scope: ArticleSearchScope, query: ArticleQuery): List<ArticleRecord> {
        calls += scope to query
        return records(query)
    }
}
