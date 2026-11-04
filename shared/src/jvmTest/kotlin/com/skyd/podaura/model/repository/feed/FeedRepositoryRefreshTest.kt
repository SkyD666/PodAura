package com.skyd.podaura.model.repository.feed

import androidx.paging.PagingConfig
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.db.dao.FeedDao
import com.skyd.podaura.model.repository.article.DownloadArticleProtectionResolver
import com.skyd.podaura.model.repository.article.IArticleRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeedRepositoryRefreshTest {
    @Test
    fun simultaneousEntryPointsShareProgressAndOnlyRefreshUnmutedFeedsOnce() = runTest {
        val finish = CompletableDeferred<Unit>()
        val requestStarted = CompletableDeferred<Unit>()
        var requests = 0
        var started = 0
        val repository = repository { urls, full ->
            assertEquals(listOf("active"), urls)
            assertFalse(full)
            flow {
                requests++
                requestStarted.complete(Unit)
                finish.await()
                emit(Unit)
            }
        }
        val first = launch(start = CoroutineStart.UNDISPATCHED) {
            assertTrue(repository.refreshAll { started++ })
        }
        requestStarted.await()
        assertTrue(repository.refreshing.value)
        assertFalse(repository.refreshAll { error("Duplicate start event") })
        assertEquals(1, requests)
        assertEquals(1, started)
        finish.complete(Unit)
        first.join()
        assertFalse(repository.refreshing.value)
    }

    @Test
    fun failureAndCancellationReleaseTheSharedRefreshForRetry() = runTest {
        val finish = CompletableDeferred<Unit>()
        var fail = false
        val repository = repository { _, _ -> flow {
            if (fail) error("network error")
            finish.await()
            emit(Unit)
        } }
        val first = launch(start = CoroutineStart.UNDISPATCHED) { repository.refreshAll {} }
        assertTrue(repository.refreshing.value)
        first.cancelAndJoin()
        assertFalse(repository.refreshing.value)
        fail = true
        assertFailsWith<IllegalStateException> { repository.refreshAll {} }
        assertFalse(repository.refreshing.value)
        fail = false
        finish.complete(Unit)
        assertTrue(repository.refreshAll {})
        assertFalse(repository.refreshing.value)
    }

    private fun repository(refresh: (List<String>, Boolean) -> Flow<Unit>): FeedRepository {
        val feeds = Proxy.newProxyInstance(
            FeedDao::class.java.classLoader, arrayOf(FeedDao::class.java),
        ) { _, method, _ ->
            check(method.name == "getAllFeedList") { "Unexpected call: ${method.name}" }
            flowOf(listOf(
                FeedBean(url = "active"), FeedBean(url = "muted", mute = true),
            ))
        } as FeedDao
        @Suppress("UNCHECKED_CAST")
        val articles = Proxy.newProxyInstance(
            IArticleRepository::class.java.classLoader, arrayOf(IArticleRepository::class.java),
        ) { _, method, args ->
            check(method.name == "refreshArticleList") { "Unexpected call: ${method.name}" }
            refresh(args[0] as List<String>, args[1] as Boolean)
        } as IArticleRepository
        return FeedRepository(
            groupDao = unused(),
            feedDao = feeds,
            articleDao = unused(),
            rssHelper = RssHelper {},
            pagingConfig = PagingConfig(pageSize = 10),
            feedSheetRepository = unused(),
            downloadArticleProtectionResolver = DownloadArticleProtectionResolver(unused(), unused()),
            articleRepo = articles,
        )
    }

    private inline fun <reified T> unused(): T = Proxy.newProxyInstance(
        T::class.java.classLoader, arrayOf(T::class.java),
    ) { _, method, _ -> error("Unexpected access: ${method.name}") } as T
}
