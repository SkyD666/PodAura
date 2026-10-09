package com.skyd.podaura.model.db.dao

import androidx.room3.Room
import androidx.room3.executeSQL
import androidx.room3.useReaderConnection
import androidx.room3.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.skyd.podaura.model.bean.ArticleNotificationRuleBean
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.article.ArticleWithEnclosureBean
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.bean.feed.FeedWithArticleBean
import com.skyd.podaura.model.db.AppDatabase
import com.skyd.podaura.model.db.instance
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedNotificationTest {
    private lateinit var directory: File
    private lateinit var database: AppDatabase
    private val url = "https://example.com/feed"

    private fun openDatabase() = AppDatabase.instance(
        Room.databaseBuilder<AppDatabase>(File(directory, "test.db").absolutePath)
            .setDriver(BundledSQLiteDriver())
    )

    @BeforeTest
    fun setUp() {
        directory = Files.createTempDirectory("feed-notification-test").toFile()
        database = openDatabase()
        startKoin {
            modules(module {
                factory { database.feedDao() }
                factory { database.articleDao() }
                factory { database.articleNotificationRuleDao() }
            })
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        database.close()
        directory.deleteRecursively()
    }

    @Test
    fun defaultsToOneManagedRuleAndDoesNotReenableAfterRefreshOrReimport() = runTest {
        database.feedDao().setFeed(FeedBean(url = url))
        repeat(3) { database.feedDao().updateFeedNotificationsEnabled(url, true) }
        assertEquals(
            1,
            database.articleNotificationRuleDao().getAllArticleNotificationRules().first().size
        )
        database.feedDao().updateFeedNotificationsEnabled(url, false)
        database.close()
        database = openDatabase()
        assertFalse(database.feedDao().observeNotificationsEnabled(url).first())
        database.feedDao().updateFeed(FeedBean(url = url, title = "Refreshed"))
        database.feedDao().setFeed(FeedBean(url = url, title = "Imported"))
        assertFalse(database.feedDao().observeNotificationsEnabled(url).first())
        database.feedDao().updateFeedNotificationsEnabled(url, true)
        assertTrue(database.feedDao().observeNotificationsEnabled(url).first())
    }

    @Test
    fun refreshingExistingArticlesOnlyNotifiesNewArticlesOnce() = runBlocking {
        val notificationIds = CompletableDeferred<List<String>>()
        val autoDownloadIds = CompletableDeferred<List<String>>()
        val articleDao = database.articleDao()
        loadKoinModules(module {
            single<ArticleDao> {
                object : ArticleDao by articleDao {
                    override suspend fun getArticleWithEnclosureListByIds(
                        articleIds: List<String>,
                    ): List<ArticleWithEnclosureBean> {
                        notificationIds.complete(articleIds)
                        return emptyList() // Capture notification candidates without posting OS notifications.
                    }

                    override suspend fun getArticleListByIds(articleIds: List<String>): List<ArticleBean> {
                        autoDownloadIds.complete(articleIds)
                        return emptyList()
                    }
                }
            }
            single { database.enclosureDao() }
            single { database.autoDownloadRuleDao() }
        })
        database.feedDao().setFeed(FeedBean(url))
        val existingArticles = listOf(
            ArticleBean("existing-guid", url, guid = "stable-guid", isRead = true, isFavorite = true),
            ArticleBean("existing-link", url, link = "$url/old", isRead = true, isFavorite = true),
        )
        existingArticles.forEach { articleDao.innerUpsertArticle(it) }
        fun ArticleBean.withEnclosures() = ArticleWithEnclosureBean(this, emptyList(), emptyList(), null)
        val refreshedArticles = existingArticles.map {
            it.copy(articleId = "parsed-${it.articleId}", title = "Updated", isRead = false, isFavorite = false)
                .withEnclosures()
        }
        val newArticle = ArticleBean("new", url, guid = "new-guid").withEnclosures()
        val repeatedArticle = newArticle.copy(article = newArticle.article.copy(articleId = "parsed-new"))
        // A full refresh can include old entries and repeated entries in the same response.
        assertTrue(
            database.feedDao().updateFeedWithArticleIfExists(
                FeedWithArticleBean(FeedBean(url), refreshedArticles + newArticle + repeatedArticle)
            )
        )
        // Refreshing the same response again must not enqueue any existing articles.
        assertTrue(
            database.feedDao().updateFeedWithArticleIfExists(
                FeedWithArticleBean(FeedBean(url), refreshedArticles + newArticle)
            )
        )
        existingArticles.forEach { original ->
            val stored = articleDao.getArticleListByIds(listOf(original.articleId)).single()
            assertEquals("Updated", stored.title)
            assertTrue(stored.isRead)
            assertTrue(stored.isFavorite)
        }
        assertEquals("new", articleDao.queryArticleByGuid("new-guid", url)?.articleId)
        withTimeout(30_000) {
            assertEquals(listOf("new"), notificationIds.await())
            assertEquals(listOf("new"), autoDownloadIds.await())
        }
    }

    @Test
    fun disablingDeletesOnlyManagedRuleAndOtherRulesStillMatch() = runTest {
        database.feedDao().setFeed(FeedBean(url = url, mute = true))
        database.articleDao().innerUpsertArticle(
            ArticleBean(
                articleId = "episode",
                feedUrl = url,
                title = "Latest news"
            )
        )
        val ruleDao = database.articleNotificationRuleDao()
        ruleDao.saveUserRule(ArticleNotificationRuleBean(name = "News", regex = ".*news.*"))
        database.feedDao().updateFeedNotificationsEnabled(url, false)
        val article =
            database.articleDao().getArticleWithEnclosureListByIds(listOf("episode")).single()
        val remaining = ruleDao.getAllArticleNotificationRules().first()
        assertEquals(1, remaining.size)
        assertFalse(remaining.single().isManaged)
        assertTrue(remaining.any { it.match(article) })
        assertEquals(
            "episode",
            database.articleDao().getArticleListByIds(listOf("episode")).single().articleId
        )
        database.feedDao().updateFeedNotificationsEnabled(url, true)
        val managed = ruleDao.getAllArticleNotificationRules().first().single { it.isManaged }
        ruleDao.removeArticleNotificationRule(managed.id)
        assertFalse(database.feedDao().observeNotificationsEnabled(url).first())
        assertEquals(remaining, ruleDao.getAllArticleNotificationRules().first())
    }

    @Test
    fun userEditsCannotModifyManagedRulesOrSaveEmptyRules() = runTest {
        database.feedDao().setFeed(FeedBean(url = url))
        val dao = database.articleNotificationRuleDao()
        val managed = dao.getAllArticleNotificationRules().first().single()
        assertFailsWith<IllegalArgumentException> { dao.saveUserRule(managed.copy(name = "Changed")) }
        assertFailsWith<IllegalArgumentException> {
            dao.saveUserRule(managed.copy(isManaged = false))
        }
        assertFailsWith<IllegalArgumentException> {
            dao.saveUserRule(ArticleNotificationRuleBean(name = "Empty", regex = ""))
        }
        assertFailsWith<IllegalArgumentException> {
            dao.saveUserRule(ArticleNotificationRuleBean(name = "Invalid", regex = "["))
        }
        dao.saveUserRule(
            ArticleNotificationRuleBean(
                name = "Scopes", regex = "",
                feedUrls = listOf(url, "other"), groupIds = listOf("a", "b")
            )
        )
        val ordinary = dao.getAllArticleNotificationRules().first().single { !it.isManaged }
        assertEquals(listOf(url, "other"), ordinary.feedUrls)
        assertEquals(listOf("a", "b"), ordinary.groupIds)
        dao.saveUserRule(ordinary.copy(name = "Edited", regex = ".*podcast.*"))
        assertEquals("Edited", dao.getRule(ordinary.id)?.name)
        database.feedDao().removeFeed(url)
        assertEquals(1, dao.getAllArticleNotificationRules().first().size)
        assertEquals(listOf("other"), dao.getRule(ordinary.id)?.feedUrls)
    }

    @Test
    fun urlChangesKeepOrdinaryRuleTargetsWithoutBroadeningScope() = runTest {
        database.feedDao().setFeed(FeedBean(url))
        database.feedDao().updateFeedNotificationsEnabled(url, false)
        val dao = database.articleNotificationRuleDao()
        dao.saveUserRule(
            ArticleNotificationRuleBean(
                name = "Scoped",
                regex = "",
                feedUrls = listOf(url, "other")
            )
        )
        database.feedDao().replaceFeedUrl(
            url, FeedWithArticleBean(FeedBean("new"), emptyList())
        )
        assertEquals(
            listOf("new", "other"),
            dao.getAllArticleNotificationRules().first().single().feedUrls
        )
        assertNull(database.feedDao().getFeed(url))
        assertFalse(database.feedDao().observeNotificationsEnabled("new").first())
    }

    @Test
    fun deletionRemovesExhaustedRulesWithoutBroadeningRegexOrGroupScope() = runTest {
        database.feedDao().setFeed(FeedBean(url))
        val dao = database.articleNotificationRuleDao()
        for ((regex, groupIds) in listOf("" to emptyList(), ".*news.*" to listOf("group"))) {
            dao.saveUserRule(
                ArticleNotificationRuleBean(
                    name = "Only this feed", regex = regex,
                    feedUrls = listOf(url), groupIds = groupIds,
                )
            )
        }
        val global = ArticleNotificationRuleBean(name = "All feeds", regex = ".*news.*")
        dao.saveUserRule(global)
        database.feedDao().removeFeed(url)
        val remaining = dao.getAllArticleNotificationRules().first()
        assertEquals(listOf(global.name), remaining.map { it.name })
        assertTrue(remaining.single().feedUrls.isEmpty())
    }

    @Test
    fun deletingFeedCascadesThroughAllRelatedTablesAndPreservesOtherFeeds() = runTest {
        insertRelatedData(url, "group")
        insertRelatedData("survivor", "other")

        assertEquals(1, database.feedDao().removeFeed(FeedBean(url)))

        assertOnlySurvivorRemains()
    }

    @Test
    fun deletingGroupCleansEveryFeedAndNotificationTarget() = runTest {
        insertRelatedData(url, "group")
        insertRelatedData("second", "group")
        insertRelatedData("survivor", "other")
        val dao = database.articleNotificationRuleDao()
        dao.saveUserRule(
            ArticleNotificationRuleBean(
                name = "Shared", regex = "", feedUrls = listOf(url, "second", "survivor"),
            )
        )
        dao.saveUserRule(
            ArticleNotificationRuleBean(
                name = "Deleted group feeds", regex = ".*", feedUrls = listOf(url, "second"),
            )
        )

        assertEquals(2, database.groupDao().removeGroupWithFeed("group"))

        assertOnlySurvivorRemains()
        val rules = dao.getAllArticleNotificationRules().first()
        assertEquals(2, rules.size)
        assertTrue(rules.all { it.feedUrls == listOf("survivor") })
    }

    @Test
    fun failedDeletionAndUrlReplacementRollBackFeedDataAndNotificationRules() = runTest {
        insertRelatedData(url, "group")
        val dao = database.articleNotificationRuleDao()
        dao.saveUserRule(
            ArticleNotificationRuleBean(name = "Scoped", regex = "", feedUrls = listOf(url))
        )
        val originalRules = dao.getAllArticleNotificationRules().first()
        database.useWriterConnection {
            it.executeSQL(
                "CREATE TRIGGER reject_delete BEFORE DELETE ON Article " +
                        "BEGIN SELECT RAISE(ABORT, 'Cannot delete'); END"
            )
        }
        assertFails { database.feedDao().removeFeed(url) }
        assertEquals(originalRules, dao.getAllArticleNotificationRules().first())

        database.useWriterConnection {
            it.executeSQL("DROP TRIGGER reject_delete")
            it.executeSQL(
                "CREATE TRIGGER reject_insert BEFORE INSERT ON Feed " +
                        "BEGIN SELECT RAISE(ABORT, 'Cannot insert'); END"
            )
        }
        assertFails {
            database.feedDao().replaceFeedUrl(
                url, FeedWithArticleBean(FeedBean("new"), emptyList())
            )
        }
        assertNotNull(database.feedDao().getFeed(url))
        assertNull(database.feedDao().getFeed("new"))
        assertEquals(url, database.articleDao().getArticleListByIds(listOf(url)).single().feedUrl)
        assertEquals(originalRules, dao.getAllArticleNotificationRules().first())
    }

    private suspend fun insertRelatedData(feedUrl: String, groupId: String) {
        database.feedDao().setFeed(FeedBean(feedUrl, groupId = groupId))
        database.articleDao().innerUpsertArticle(
            ArticleBean(articleId = feedUrl, feedUrl = feedUrl, isFavorite = true)
        )
        database.useWriterConnection { connection ->
            connection.executeSQL(
                "INSERT OR IGNORE INTO Playlist VALUES ('playlist', 'Playlist', 0, 0, 0)"
            )
            connection.usePrepared(
                "INSERT OR IGNORE INTO `Group` VALUES (?, 'Group', 1, 0)"
            ) { statement ->
                statement.bindText(1, groupId)
                statement.step()
            }
            for (sql in listOf(
                "INSERT INTO enclosure VALUES (?1, 'media', 0, 'audio/mpeg')",
                "INSERT INTO ArticleCategory VALUES (?1, 'category')",
                "INSERT INTO RssMedia (articleId, adult) VALUES (?1, 0)",
                "INSERT INTO ReadHistory VALUES (?1, 0)",
                "INSERT INTO MediaPlayHistory VALUES (?1, 100, 10, 0, ?1)",
                "INSERT INTO PlaylistMedia VALUES ('playlist', ?1, ?1, 0, 0)",
                "INSERT INTO AutoDownloadRule VALUES (?1, 0, 0, 0, 1, 10, NULL)",
            )) {
                connection.usePrepared(sql) { statement ->
                    statement.bindText(1, feedUrl)
                    statement.step()
                }
            }
        }
    }

    private suspend fun assertOnlySurvivorRemains() {
        database.useReaderConnection { connection ->
            for (table in listOf(
                "Feed", "Article", "enclosure", "ArticleCategory", "RssMedia",
                "ReadHistory", "MediaPlayHistory", "PlaylistMedia", "AutoDownloadRule",
            )) {
                connection.usePrepared("SELECT * FROM $table") { statement ->
                    assertTrue(statement.step(), "$table should retain the other feed's data")
                    assertEquals(
                        "survivor", statement.getText(if (table == "PlaylistMedia") 1 else 0), table
                    )
                    assertFalse(statement.step(), "$table should not retain deleted feed data")
                }
            }
            connection.usePrepared("PRAGMA foreign_key_check") { statement ->
                assertFalse(statement.step(), "No dangling foreign keys")
            }
        }
        assertFalse(database.feedDao().observeNotificationsEnabled(url).first())
        assertTrue(database.feedDao().observeNotificationsEnabled("survivor").first())
    }

    @Test
    fun managedLookupUsesExactSingletonAndDoesNotDeleteOrdinaryRules() = runTest {
        val escapedUrl = "https://example.com/feed?q=\"quote\"&path=\\folder"
        val sibling = "$escapedUrl/other"
        database.feedDao().setFeed(FeedBean(url = escapedUrl))
        database.feedDao().setFeed(FeedBean(url = sibling))
        val dao = database.articleNotificationRuleDao()
        repeat(2) { index ->
            dao.saveUserRule(
                ArticleNotificationRuleBean(
                    name = "Ordinary $index", regex = ".*",
                    feedUrls = listOf(escapedUrl)
                )
            )
        }
        database.feedDao().updateFeedNotificationsEnabled(escapedUrl, false)
        assertFalse(database.feedDao().observeNotificationsEnabled(escapedUrl).first())
        assertTrue(database.feedDao().observeNotificationsEnabled(sibling).first())
        assertEquals(2, dao.getAllArticleNotificationRules().first().count { !it.isManaged })
        coroutineScope {
            repeat(10) {
                launch {
                    database.feedDao().updateFeedNotificationsEnabled(escapedUrl, true)
                }
            }
        }
        val rules = dao.getAllArticleNotificationRules().first()
        assertEquals(1, rules.count { it.isManaged && it.feedUrls == listOf(escapedUrl) })
        assertEquals(1, rules.count { it.isManaged && it.feedUrls == listOf(sibling) })
    }
}
