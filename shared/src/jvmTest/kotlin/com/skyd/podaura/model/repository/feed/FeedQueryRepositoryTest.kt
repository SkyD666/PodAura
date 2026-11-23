package com.skyd.podaura.model.repository.feed

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.skyd.podaura.api.model.bean.KeywordMatch
import com.skyd.podaura.api.model.bean.feed.FeedQuery
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.db.AppDatabase
import com.skyd.podaura.model.db.instance
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class FeedQueryRepositoryTest {
    @Test
    fun queriesSavedTitlesAliasesDescriptionsAndHardFiltersWithLiteralKeywords() = runTest {
        val directory = Files.createTempDirectory("feed-query-test").toFile()
        val database = AppDatabase.instance(Room.databaseBuilder<AppDatabase>(directory.resolve("test.db").path)
            .setDriver(BundledSQLiteDriver()))
        try {
            val dao = database.feedDao()
            dao.innerSetFeed(FeedBean("https://a.test", title = "Music", nickname = "Alias", description = "Jazz", mute = true))
            dao.innerSetFeed(FeedBean("https://b.test", title = "100%_literal!", customDescription = "Custom music"))
            dao.innerSetFeed(FeedBean("https://c.test/token=secret", title = "Other", requestHeaders = FeedBean.RequestHeaders(mapOf("secret" to "Music"))))
            val repository = FeedQueryRepository(dao)
            suspend fun urls(query: FeedQuery) = repository.query(query).map { it.url }.toSet()
            assertEquals(setOf("https://a.test", "https://b.test"), urls(FeedQuery(keywords = listOf("music"))))
            assertEquals(setOf("https://a.test"), urls(FeedQuery(keywords = listOf("Alias", "Jazz"))))
            assertEquals(setOf("https://a.test", "https://b.test"), urls(FeedQuery(keywords = listOf("Jazz", "Custom"), match = KeywordMatch.ANY)))
            assertEquals(setOf("https://b.test"), urls(FeedQuery(keywords = listOf("%_literal!"))))
            assertTrue(urls(FeedQuery(keywords = listOf("' OR 1=1 --"))).isEmpty())
            assertTrue(urls(FeedQuery(keywords = listOf("secret"))).isEmpty())
            assertEquals(setOf("https://a.test"), urls(FeedQuery(muted = true)))
            assertTrue(urls(FeedQuery(withinFeedUrls = emptyList())).isEmpty())
            assertTrue(urls(FeedQuery(muted = false, withinFeedUrls = listOf("https://a.test"))).isEmpty())
            assertEquals(setOf("https://b.test"), urls(FeedQuery(withinFeedUrls = listOf("https://b.test"))))
            assertEquals(1, repository.query(FeedQuery(limit = 1)).size)
            val record = repository.query(FeedQuery(withinFeedUrls = listOf("https://a.test"))).single()
            assertEquals("Alias", record.nickname)
            assertEquals("Music", record.title)
            dao.innerRemoveFeed("https://a.test")
            assertTrue(urls(FeedQuery(withinFeedUrls = listOf("https://a.test"))).isEmpty())
        } finally {
            database.close()
            directory.deleteRecursively()
        }
    }
}
