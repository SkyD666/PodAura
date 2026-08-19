package com.skyd.podaura.model.db.dao

import androidx.room3.Room
import androidx.room3.RoomRawQuery
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.skyd.podaura.model.bean.article.ARTICLE_TABLE_NAME
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.db.AppDatabase
import com.skyd.podaura.model.db.instance
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ArticleDaoDownloadProtectionTest {
    private lateinit var databaseDirectory: File
    private lateinit var database: AppDatabase
    private lateinit var articleDao: ArticleDao

    @BeforeTest
    fun setUp() {
        databaseDirectory = Files.createTempDirectory("podaura-article-delete-test").toFile()
        database = AppDatabase.instance(
            Room.databaseBuilder<AppDatabase>(
                name = File(databaseDirectory, "articles.db").absolutePath,
            ).setDriver(BundledSQLiteDriver())
        )
        articleDao = database.articleDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
        databaseDirectory.deleteRecursively()
    }

    @Test
    fun feedDeletionProtectsMatchingArticleAndAllowsEmptyProtectionList() = runTest {
        insertArticles(
            feedUrl = "https://example.com/feed.xml",
            "delete-1" to 1L,
            "protected" to 2L,
            "delete-2" to 3L,
        )

        val protectedResult = articleDao.deleteArticleInFeed(
            feedUrl = "https://example.com/feed.xml",
            keepPlaylistArticles = false,
            keepUnread = false,
            keepFavorite = false,
            downloadProtectedArticleIds = listOf("protected"),
        )

        assertEquals(2, protectedResult.deletedCount)
        assertEquals(1, protectedResult.downloadProtectedCount)
        assertEquals(setOf("protected"), articleIds())

        val unprotectedResult = articleDao.deleteArticle(
            articleId = "protected",
            keepPlaylistArticles = false,
            keepUnread = false,
            keepFavorite = false,
            downloadProtectedArticleIds = emptyList(),
        )

        assertEquals(1, unprotectedResult.deletedCount)
        assertEquals(0, unprotectedResult.downloadProtectedCount)
        assertEquals(emptySet(), articleIds())
    }

    @Test
    fun dateDeletionReportsOnlyProtectedArticlesInItsCandidateRange() = runTest {
        insertArticles(
            feedUrl = "https://example.com/date.xml",
            "old-delete" to 10L,
            "old-protected" to 20L,
            "new-protected" to 200L,
        )

        val result = articleDao.deleteArticleBefore(
            timestamp = 100L,
            keepPlaylistArticles = false,
            keepUnread = false,
            keepFavorite = false,
            downloadProtectedArticleIds = listOf("old-protected", "new-protected"),
        )

        assertEquals(1, result.deletedCount)
        assertEquals(1, result.downloadProtectedCount)
        assertEquals(setOf("old-protected", "new-protected"), articleIds())
    }

    @Test
    fun maxCountDeletionKeepsProtectedArticleWithoutChangingRanking() = runTest {
        insertArticles(
            feedUrl = "https://example.com/count.xml",
            "oldest" to 1L,
            "protected-middle" to 2L,
            "newest" to 3L,
        )

        val result = articleDao.deleteArticleExceed(
            count = 1,
            keepPlaylistArticles = false,
            keepUnread = false,
            keepFavorite = false,
            downloadProtectedArticleIds = listOf("protected-middle"),
        )

        assertEquals(1, result.deletedCount)
        assertEquals(1, result.downloadProtectedCount)
        assertEquals(setOf("protected-middle", "newest"), articleIds())
    }

    @Test
    fun feedDeletionHandlesMoreIdsThanAndroidSqliteBindLimit() = runTest {
        val feedUrl = "https://example.com/large.xml"
        val articles = (0..901).map { index ->
            "article-$index" to index.toLong()
        }.toTypedArray()
        insertArticles(feedUrl, *articles)
        val protectedIds = buildList {
            add("article-0")
            repeat(900) { index -> add("missing-$index") }
        }

        val result = articleDao.deleteArticleInFeed(
            feedUrl = feedUrl,
            keepPlaylistArticles = false,
            keepUnread = false,
            keepFavorite = false,
            downloadProtectedArticleIds = protectedIds,
        )

        assertEquals(901, result.deletedCount)
        assertEquals(1, result.downloadProtectedCount)
        assertEquals(setOf("article-0"), articleIds())
    }

    private suspend fun insertArticles(
        feedUrl: String,
        vararg articles: Pair<String, Long>,
    ) {
        database.feedDao().setFeed(FeedBean(url = feedUrl))
        articles.forEach { (articleId, updateAt) ->
            articleDao.innerUpsertArticle(
                ArticleBean(
                    articleId = articleId,
                    feedUrl = feedUrl,
                    updateAt = updateAt,
                )
            )
        }
    }

    private suspend fun articleIds(): Set<String> = articleDao.getArticleList(
        RoomRawQuery("SELECT * FROM $ARTICLE_TABLE_NAME")
    ).map { it.articleWithEnclosure.article.articleId }.toSet()
}
