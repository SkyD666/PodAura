package com.skyd.podaura.model.worker.rsssync

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RssSyncFeedRotationTest {
    @Test
    fun expiredRefreshDoesNotLetFiveSlowFeedsStarveTheNextFeed() = runTest {
        var persistedStartingFeed: String? = null
        val feeds = listOf("a", "b", "c", "d", "e", "f")
        val refreshed = mutableListOf<String>()
        suspend fun refreshWindow() {
            // Only the saved starting feed survives between refresh windows.
            val ordered = rotateRssSyncFeeds(feeds.reversed(), persistedStartingFeed)
            persistedStartingFeed = ordered.firstOrNull()
            withTimeoutOrNull(20.seconds) {
                coroutineScope {
                    val semaphore = Semaphore(5)
                    ordered.forEach { feed ->
                        launch {
                            semaphore.withPermit {
                                if (feed != "f") awaitCancellation()
                                refreshed += feed
                            }
                        }
                    }
                }
            }
        }
        refreshWindow()
        assertTrue(refreshed.isEmpty())
        refreshWindow()
        assertEquals(listOf("f"), refreshed)
    }

    @Test
    fun removedStartingFeedAndWraparoundKeepAllCurrentSubscriptions() {
        assertEquals(listOf("c", "d", "a"), rotateRssSyncFeeds(listOf("d", "a", "c"), "b"))
        assertEquals(listOf("a", "c", "d"), rotateRssSyncFeeds(listOf("d", "a", "c"), "d"))
        assertTrue(rotateRssSyncFeeds(emptyList(), "b").isEmpty())
    }
}
