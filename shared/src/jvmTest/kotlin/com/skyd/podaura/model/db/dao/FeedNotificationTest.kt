package com.skyd.podaura.model.db.dao

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.skyd.podaura.model.bean.ArticleNotificationRuleBean
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.db.AppDatabase
import com.skyd.podaura.model.db.instance
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
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
    }

    @AfterTest
    fun tearDown() {
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
    }

    @Test
    fun urlChangesKeepOrdinaryRuleTargetsWithoutBroadeningScope() = runTest {
        val dao = database.articleNotificationRuleDao()
        dao.saveUserRule(
            ArticleNotificationRuleBean(
                name = "Scoped",
                regex = "",
                feedUrls = listOf(url, "other")
            )
        )
        dao.moveUserFeedTargets(url, "new")
        assertEquals(
            listOf("new", "other"),
            dao.getAllArticleNotificationRules().first().single().feedUrls
        )
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
