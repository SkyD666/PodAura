package com.skyd.podaura.ui.screen.feed

import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.bean.feed.FeedViewBean
import com.skyd.podaura.ui.screen.feed.sheet.FeedSheetIntent
import com.skyd.podaura.ui.screen.feed.sheet.FeedSheetPartialStateChange
import com.skyd.podaura.ui.screen.feed.sheet.FeedSheetState
import com.skyd.podaura.ui.screen.feed.sheet.refreshFeedChanges
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RefreshFeedFlowTest {
    private val feed = FeedViewBean(FeedBean("https://example.com/feed.xml"))

    @Test
    fun groupCancellationStopsRequestsWaitsForSavingAndAllowsRetry() = runTest {
        val intents = Channel<FeedIntent>(Channel.UNLIMITED)
        val changes = mutableListOf<FeedPartialStateChange.RefreshFeed>()
        val saved = CompletableDeferred<Unit>()
        var stoppedRequests = 0
        val attempts = mutableListOf<FeedIntent.RefreshGroupFeed>()
        intents.receiveAsFlow().refreshGroupFeedChanges { intent ->
            flow {
                attempts += intent
                if (attempts.size == 1) {
                    coroutineScope {
                        repeat(2) {
                            launch {
                                try { awaitCancellation() } finally { stoppedRequests++ }
                            }
                        }
                        launch { withContext(NonCancellable) { saved.await() } }
                    }
                }
                emit(listOf(feed))
            }
        }.onEach(changes::add).launchIn(backgroundScope)

        val refresh = FeedIntent.RefreshGroupFeed("group", full = false)
        intents.send(refresh)
        runCurrent()
        intents.send(FeedIntent.CancelAddFeed)
        runCurrent()
        assertEquals(FeedPartialStateChange.RefreshFeed.Loading, changes.last())
        intents.send(refresh)
        intents.send(FeedIntent.CancelRefreshGroupFeed)
        runCurrent()
        assertEquals(1, attempts.size)
        assertEquals(2, stoppedRequests)
        assertEquals(FeedPartialStateChange.RefreshFeed.Cancelling, changes.last())
        var state = FeedState.initial().copy(
            loadingDialog = true, refreshAllFeedsInProgress = true, editFeedUrl = feed.feed.url,
        )
        changes.forEach { state = it.reduce(state) }
        assertEquals(RefreshFeedState.Cancelling, state.refreshFeedState)

        saved.complete(Unit)
        runCurrent()
        assertEquals(FeedPartialStateChange.RefreshFeed.Cancelled, changes.last())
        state = changes.last().reduce(state)
        assertNull(state.refreshFeedState)
        assertTrue(state.loadingDialog)
        assertTrue(state.refreshAllFeedsInProgress)
        assertEquals(feed.feed.url, state.editFeedUrl)
        assertFalse(changes.any { it is FeedPartialStateChange.RefreshFeed.Failed })

        intents.send(refresh.copy(full = true))
        runCurrent()
        assertEquals(listOf(refresh, refresh.copy(full = true)), attempts)
        assertEquals(FeedPartialStateChange.RefreshFeed.Success(listOf(feed)), changes.last())
        val count = changes.size
        intents.send(FeedIntent.CancelRefreshGroupFeed)
        runCurrent()
        assertEquals(count, changes.size)
    }

    @Test
    fun singleFeedFailureCancellationAndRetryKeepTheSheetOpen() = runTest {
        val intents = Channel<FeedSheetIntent>(Channel.UNLIMITED)
        val changes = mutableListOf<FeedSheetPartialStateChange.RefreshFeed>()
        val lateResult = CompletableDeferred<Unit>()
        val updatedFeed = feed.copy(feed = feed.feed.copy(
            title = "Updated title", description = "Updated description", icon = "https://example.com/new.png",
        ))
        var persistedFeed = feed
        var reloads = 0
        val attempts = mutableListOf<FeedSheetIntent.RefreshFeed>()
        intents.receiveAsFlow().refreshFeedChanges(
            reloadFeed = { url ->
                assertEquals(feed.feed.url, url)
                reloads++
                persistedFeed
            },
        ) { intent ->
            flow {
                attempts += intent
                when (attempts.size) {
                    1 -> error("Network unavailable")
                    2 -> withContext(NonCancellable) {
                        lateResult.await()
                        persistedFeed = updatedFeed
                    }
                }
                emit(listOf(feed))
            }
        }.onEach(changes::add).launchIn(backgroundScope)

        val refresh = FeedSheetIntent.RefreshFeed(feed.feed.url, full = false)
        intents.send(refresh)
        runCurrent()
        assertEquals(FeedSheetPartialStateChange.RefreshFeed.Failed("Network unavailable"), changes.last())
        assertNull(changes.last().reduce(FeedSheetState.initial()).refreshFeedState)

        intents.send(refresh.copy(full = true))
        runCurrent()
        intents.send(refresh.copy(url = "https://example.com/ignored.xml"))
        intents.send(FeedSheetIntent.CancelRefreshFeed)
        runCurrent()
        assertEquals(2, attempts.size)
        assertEquals(FeedSheetPartialStateChange.RefreshFeed.Cancelling, changes.last())
        assertEquals(0, reloads)
        lateResult.complete(Unit)
        runCurrent()
        assertEquals(1, reloads)
        assertEquals(FeedSheetPartialStateChange.RefreshFeed.Cancelled(updatedFeed), changes.last())
        assertFalse(changes.any { it is FeedSheetPartialStateChange.RefreshFeed.Success })
        val state = changes.last().reduce(
            FeedSheetState.initial().copy(editFeedDialogBean = feed, loadingDialog = true),
        )
        assertNull(state.refreshFeedState)
        assertEquals(updatedFeed, state.editFeedDialogBean)
        assertTrue(state.loadingDialog)
        assertNull(changes.last().reduce(FeedSheetState.initial()).editFeedDialogBean)
        val otherFeed = FeedViewBean(FeedBean("https://example.com/other.xml"))
        assertEquals(otherFeed, changes.last().reduce(state.copy(editFeedDialogBean = otherFeed)).editFeedDialogBean)

        intents.send(refresh)
        runCurrent()
        assertEquals(listOf(refresh, refresh.copy(full = true), refresh), attempts)
        assertEquals(FeedSheetPartialStateChange.RefreshFeed.Success(listOf(feed)), changes.last())
    }

    @Test
    fun reloadFailureStillEndsCancellationAndAllowsRetry() = runTest {
        val intents = Channel<FeedSheetIntent>(Channel.UNLIMITED)
        val changes = mutableListOf<FeedSheetPartialStateChange.RefreshFeed>()
        var attempts = 0
        intents.receiveAsFlow().refreshFeedChanges(
            reloadFeed = { error("Database unavailable") },
        ) {
            flow {
                if (++attempts == 1) awaitCancellation()
                emit(listOf(feed))
            }
        }.onEach(changes::add).launchIn(backgroundScope)

        val refresh = FeedSheetIntent.RefreshFeed(feed.feed.url, full = false)
        intents.send(refresh)
        runCurrent()
        intents.send(FeedSheetIntent.CancelRefreshFeed)
        runCurrent()
        assertEquals(FeedSheetPartialStateChange.RefreshFeed.Cancelled(null), changes.last())
        val state = changes.last().reduce(
            FeedSheetState.initial().copy(editFeedDialogBean = feed, refreshFeedState = RefreshFeedState.Cancelling),
        )
        assertEquals(feed, state.editFeedDialogBean)
        assertNull(state.refreshFeedState)
        assertFalse(changes.any { it is FeedSheetPartialStateChange.RefreshFeed.Failed })

        intents.send(refresh)
        runCurrent()
        assertEquals(FeedSheetPartialStateChange.RefreshFeed.Success(listOf(feed)), changes.last())
    }
}
