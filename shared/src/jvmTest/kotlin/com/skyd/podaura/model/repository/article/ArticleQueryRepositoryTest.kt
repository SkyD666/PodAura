package com.skyd.podaura.model.repository.article

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.skyd.podaura.config.allSearchDomain
import com.skyd.podaura.api.model.bean.KeywordMatch
import com.skyd.podaura.api.model.bean.article.ArticleQuery
import com.skyd.podaura.api.model.bean.article.ArticleSearchScope
import com.skyd.podaura.api.model.repository.ArticleSearchFieldsDisabledException
import com.skyd.podaura.model.bean.SearchDomainBean
import com.skyd.podaura.model.bean.article.ARTICLE_TABLE_NAME
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.db.AppDatabase
import com.skyd.podaura.model.db.SearchDomainDatabase
import com.skyd.podaura.model.db.instance
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ArticleQueryRepositoryTest {
    private lateinit var directory: File
    private lateinit var database: AppDatabase
    private lateinit var domains: SearchDomainDatabase
    private lateinit var catalog: ArticleQueryRepository
    private val feed = "https://example.org/it's-feed"

    @BeforeTest
    fun setup() {
        directory = Files.createTempDirectory("article-query").toFile()
        database = AppDatabase.instance(Room.databaseBuilder<AppDatabase>(File(directory, "articles.db").absolutePath)
            .setDriver(BundledSQLiteDriver()))
        domains = SearchDomainDatabase.instance(Room.databaseBuilder<SearchDomainDatabase>(File(directory, "domains.db").absolutePath)
            .setDriver(BundledSQLiteDriver()))
        catalog = ArticleQueryRepository(database.articleDao(), domains.searchDomainDao())
    }

    @AfterTest
    fun cleanup() {
        database.close()
        domains.close()
        directory.deleteRecursively()
    }

    private suspend fun seed() {
        database.feedDao().setFeed(FeedBean(feed, title = "Podcast"))
        listOf(
            ArticleBean("a", feed, title = "Kotlin 100%_!", date = 100, content = "<p>Agent 可爱</p>", isFavorite = true),
            ArticleBean("b", feed, title = "Kotlin", date = 200, description = "Agent", isRead = true),
            ArticleBean("c", feed, title = "Android", date = 300, description = "Read", isFavorite = true),
            ArticleBean("no-date", feed, title = "Kotlin", date = null),
        ).forEach { database.articleDao().innerUpsertArticle(it) }
    }

    @Test
    fun boundLiteralSearchAllAnyOrderingLimitAndRawRows() = runTest {
        seed()
        val literal = catalog.query(ArticleSearchScope.All, ArticleQuery(keywords = listOf("100%_!")))
        assertEquals(listOf("a"), literal.map { it.articleId })
        assertEquals("<p>Agent 可爱</p>", literal.single().content)
        assertEquals("Kotlin 100%_!", literal.single().title)
        assertEquals("Podcast", literal.single().sourceTitle)
        assertTrue(catalog.query(ArticleSearchScope.All, ArticleQuery(keywords = listOf("' OR 1=1 --"))).isEmpty())
        assertEquals(listOf("b", "a"), catalog.query(ArticleSearchScope.All,
            ArticleQuery(keywords = listOf("Kotlin", "Agent"))).map { it.articleId })
        val any = catalog.query(ArticleSearchScope.All,
            ArticleQuery(keywords = listOf("Kotlin", "Android"), match = KeywordMatch.ANY, limit = 2))
        assertEquals(listOf("c", "b"), any.map { it.articleId }) // exact public query limit
        assertFalse(database.articleDao().getArticleListByIds(listOf("a")).single().isRead)
    }

    @Test
    fun scopeIntersectionEmptySetDatesAndCurrentHardFilters() = runTest {
        seed()
        val restricted = ArticleSearchScope.Restricted(articleIds = setOf("a", "c"))
        val query = ArticleQuery(read = false, favorite = true, publishedFromMillis = 100, publishedBeforeMillis = 300)
        assertEquals(listOf("a"), catalog.query(restricted, query).map { it.articleId })
        assertTrue(catalog.query(ArticleSearchScope.Restricted(), query).isEmpty())
        assertTrue(catalog.query(ArticleSearchScope.All, query.copy(withinArticleIds = emptyList())).isEmpty())
        assertTrue(catalog.query(restricted, query.copy(withinArticleIds = listOf("b"))).isEmpty())
        assertEquals(listOf("a"), catalog.query(restricted, query.copy(withinArticleIds = listOf("a", "b"))).map { it.articleId })
        database.articleDao().readArticle("a", true)
        assertTrue(catalog.query(restricted, query.copy(withinArticleIds = listOf("a"))).isEmpty())
        val largeScope = ArticleSearchScope.Restricted(
            feedUrls = (1..1200).map { "absent-$it" }.toSet() + feed, articleIds = setOf("a"),
        )
        assertEquals(listOf("c", "b", "a", "no-date"), catalog.query(largeScope, ArticleQuery()).map { it.articleId })
    }

    @Test
    fun invalidLimitsAndTooManyParametersFailBeforeSqlExecution() = runTest {
        assertFailsWith<IllegalArgumentException> {
            catalog.query(ArticleSearchScope.All, ArticleQuery(limit = -1))
        }
        assertFailsWith<IllegalArgumentException> {
            catalog.query(ArticleSearchScope.All, ArticleQuery(withinArticleIds = List(1000) { "id-$it" }))
        }
    }

    @Test
    fun disabledFieldsDoNotTurnATextSearchIntoWholeLibrary() = runTest {
        seed()
        allSearchDomain.getValue(ARTICLE_TABLE_NAME).forEach { column ->
            domains.searchDomainDao().setSearchDomain(SearchDomainBean(ARTICLE_TABLE_NAME, column, false))
        }
        domains.searchDomainDao().setSearchDomain(SearchDomainBean(ARTICLE_TABLE_NAME, "Title` OR 1=1 --", true))
        assertFailsWith<ArticleSearchFieldsDisabledException> {
            catalog.query(ArticleSearchScope.All, ArticleQuery(keywords = listOf("Kotlin")))
        }
        assertEquals(4, catalog.query(ArticleSearchScope.All, ArticleQuery()).size)
    }
}
