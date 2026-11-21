package com.skyd.podaura.podnya.capabilities.articles

import com.skyd.podaura.podnya.runtime.podnyaJson
import ai.koog.prompt.message.MessagePart
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchQuery
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchLimits
import com.skyd.podaura.api.model.bean.article.ArticleSearchScope
import com.skyd.podaura.api.model.repository.IArticleRepository
import com.skyd.podaura.api.model.bean.article.ArticleRecord
import com.skyd.podaura.podnya.capabilities.articles.model.repository.LocalArticleSearchRepository
import com.skyd.podaura.api.model.bean.article.ArticleQuery
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SearchArticlesToolTest {
    @Test
    fun invalidCallsNeverReachSource() = runTest {
        val catalog = RecordingCatalog()
        val tool = SearchArticlesTool(LocalArticleSearchRepository(catalog), ArticleSearchScope.All, TimeZone.UTC, emptySet())
        assertNotNull(tool.admit(MessagePart.Tool.Call("1", "search_articles", "not json")))
        assertNotNull(tool.admit(MessagePart.Tool.Call("2", "shell", "{}")))
        assertNotNull(tool.admit(MessagePart.Tool.Call("3", "search_articles", "{\"limit\":21}")))
        assertNull(tool.admit(MessagePart.Tool.Call("4", "search_articles", "{}")))
        assertEquals(0, catalog.queries.size)
    }

    @Test
    fun capturedZoneHardFiltersAndCandidateProvenanceSurviveRewrites() = runTest {
        val catalog = RecordingCatalog()
        val zone = TimeZone.of("America/New_York")
        val scope = ArticleSearchScope.Restricted(feedUrls = setOf("authorized"))
        val tool = SearchArticlesTool(LocalArticleSearchRepository(catalog), scope, zone, setOf("a", "b"))
        val first = ArticleSearchQuery(keywords = listOf(" Kotlin ", "Kotlin"),
            publishedFrom = "2026-03-08", publishedBefore = "2026-03-09", read = false,
            favorite = true, withinArticleIds = listOf("a", "b"), excludeArticleIds = listOf("b"))
        assertNull(tool.admit(MessagePart.Tool.Call("1", "search_articles", podnyaJson.encodeToString(first))))
        tool.execute(first)
        val resolved = catalog.queries.single()
        assertEquals(listOf("Kotlin"), resolved.keywords)
        assertEquals(LocalDate(2026, 3, 8).atStartOfDayIn(zone).toEpochMilliseconds(), resolved.publishedFromMillis)
        assertEquals(23 * 60 * 60 * 1000L, resolved.publishedBeforeMillis!! - resolved.publishedFromMillis!!)
        listOf(
            first.copy(read = null), first.copy(favorite = false),
            first.copy(publishedFrom = null), first.copy(publishedBefore = "2026-03-10"),
            first.copy(withinArticleIds = null), first.copy(excludeArticleIds = emptyList()),
            first.copy(withinArticleIds = listOf("unknown")), first.copy(publishedFrom = "2026-02-30"),
        ).forEach { assertFailsWith<IllegalArgumentException> { tool.resolve(it) } }
        val second = first.copy(keywords = listOf("Multiplatform"), withinArticleIds = listOf("a"))
        tool.execute(second)
        assertEquals(catalog.queries.last().copy(limit = ArticleSearchLimits.SEARCH_RESULTS), tool.evidence.getValue("a").query)
        assertEquals("Multiplatform", tool.records.last().query.keywords.single())
        assertEquals(listOf("a"), tool.records.last().resultIds)
        assertEquals(scope, catalog.scope)
    }

    @Test
    fun emptyWithinIdsIsNeverUnrestrictedAndBoundsAreStrict() {
        val tool = SearchArticlesTool(LocalArticleSearchRepository(RecordingCatalog()), ArticleSearchScope.All, TimeZone.UTC, setOf("a"))
        assertEquals(emptyList(), tool.resolve(ArticleSearchQuery(withinArticleIds = emptyList())).withinArticleIds)
        assertNull(tool.resolve(ArticleSearchQuery()).withinArticleIds)
        listOf(
            ArticleSearchQuery(keywords = List(7) { "term" }),
            ArticleSearchQuery(keywords = listOf("x".repeat(81))),
            ArticleSearchQuery(publishedFrom = "2026-01-02", publishedBefore = "2026-01-02"),
            ArticleSearchQuery(excludeArticleIds = List(61) { "a" }),
        ).forEach { assertFailsWith<IllegalArgumentException> { tool.resolve(it) } }
    }

    @Test
    fun searchKeepsLaterCandidatesAndSupportsAnotherPage() = runTest {
        val catalog = object : IArticleRepository {
            override suspend fun query(scope: ArticleSearchScope, query: ArticleQuery): List<ArticleRecord> {
                assertEquals(ArticleSearchLimits.SEARCH_RESULTS + 1, query.limit)
                return (1..25).map { index ->
                    ArticleRecord(
                        articleId = "a$index",
                        title = if (index == 18) "Beyond retrospective" else "Episode $index",
                        description = "Beyond",
                    )
                }.filter { it.articleId !in query.excludeArticleIds }.take(query.limit)
            }
        }
        val tool = SearchArticlesTool(
            source = LocalArticleSearchRepository(catalog),
            scope = ArticleSearchScope.All,
            timeZone = TimeZone.UTC,
            historyIds = emptySet(),
        )
        val first = tool.execute(ArticleSearchQuery(keywords = listOf("Beyond"))).page!!
        assertEquals((1..20).map { "a$it" }, first.items.map { it.articleId })
        assertTrue(first.hasMore)
        assertEquals("Beyond retrospective", tool.evidence.getValue("a18").hit.title)
        val next = tool.execute(
            ArticleSearchQuery(
                keywords = listOf("Beyond"),
                excludeArticleIds = first.items.map { it.articleId },
            )
        ).page!!
        assertEquals((21..25).map { "a$it" }, next.items.map { it.articleId })
        assertEquals(25, tool.evidence.size)
        assertEquals(false, next.hasMore)
    }
}

/** Records actual storage queries while tests use the real article catalog. */
internal class RecordingCatalog : IArticleRepository {
    val queries = mutableListOf<ArticleQuery>()
    var scope: ArticleSearchScope? = null
    override suspend fun query(scope: ArticleSearchScope, query: ArticleQuery): List<ArticleRecord> {
        this.scope = scope
        queries += query
        // Simulate metadata changing after the first read; final cards must use current data.
        val firstRead = queries.size == 1
        return listOf(ArticleRecord("a", title = if (firstRead) "Local title" else "Fresh local title",
            sourceTitle = "Local catalog", dateMillis = if (firstRead) 0L else 1_700_000_000_000L,
            content = "Local snippet"))
            .filter { query.withinArticleIds?.let { ids -> it.articleId in ids } != false }
    }
}
