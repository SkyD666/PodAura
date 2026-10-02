package com.skyd.podaura.ui.screen.feed

import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.bean.feed.FeedViewBean
import com.skyd.podaura.model.bean.group.GroupVo
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

class AddFeedFlowTest {
    private val url = "https://example.com/podcast.xml"
    private val feed = FeedViewBean(FeedBean(url))
    private val addIntent = FeedIntent.AddFeed(url, group = GroupVo("test", "Test", true))

    @Test
    fun cancellationStopsChildrenRestoresUrlAndAllowsRetry() = runTest {
        val intents = Channel<FeedIntent>(Channel.UNLIMITED)
        val changes = mutableListOf<FeedPartialStateChange.AddFeed>()
        var attempts = 0
        var stoppedRequests = 0
        intents.receiveAsFlow().addFeedChanges { _, onSaving ->
            flow {
                if (++attempts == 1) {
                    coroutineScope {
                        repeat(2) {
                            launch {
                                try {
                                    awaitCancellation()
                                } finally {
                                    stoppedRequests++
                                }
                            }
                        }
                    }
                }
                onSaving()
                emit(feed)
            }
        }.onEach(changes::add).launchIn(backgroundScope)

        intents.send(addIntent)
        runCurrent()
        intents.send(FeedIntent.CancelAddFeed)
        runCurrent()

        assertEquals(2, stoppedRequests)
        assertEquals(
            listOf(FeedPartialStateChange.AddFeed.Loading, FeedPartialStateChange.AddFeed.Cancelled(url)),
            changes,
        )
        assertNull(changes.last().reduce(changes.first().reduce(FeedState.initial())).addFeedState)

        intents.send(addIntent)
        runCurrent()
        assertEquals(2, attempts)
        assertEquals(FeedPartialStateChange.AddFeed.Success(feed), changes.last())
        assertEquals(url, changes.last().reduce(FeedState.initial()).editFeedUrl)
    }

    @Test
    fun savingRejectsCancellationAndDuplicateSubmissions() = runTest {
        val intents = Channel<FeedIntent>(Channel.UNLIMITED)
        val changes = mutableListOf<FeedPartialStateChange.AddFeed>()
        val saved = CompletableDeferred<Unit>()
        var attempts = 0
        intents.receiveAsFlow().addFeedChanges { _, onSaving ->
            flow {
                attempts++
                onSaving()
                saved.await()
                emit(feed)
            }
        }.onEach(changes::add).launchIn(backgroundScope)

        intents.send(addIntent)
        runCurrent()
        assertEquals(AddFeedState.Saving, changes.last().reduce(FeedState.initial()).addFeedState)
        intents.send(FeedIntent.CancelAddFeed)
        intents.send(addIntent)
        runCurrent()
        saved.complete(Unit)
        runCurrent()

        assertEquals(1, attempts)
        assertEquals(FeedPartialStateChange.AddFeed.Success(feed), changes.last())
        assertFalse(changes.any { it is FeedPartialStateChange.AddFeed.Cancelled })
        // An unrelated operation's waiting dialog must not be dismissed by an add result.
        assertTrue(changes.last().reduce(FeedState.initial().copy(loadingDialog = true)).loadingDialog)
    }

    @Test
    fun cancellationWinsEvenIfFetchingReturnsALateResult() = runTest {
        val intents = Channel<FeedIntent>(Channel.UNLIMITED)
        val changes = mutableListOf<FeedPartialStateChange.AddFeed>()
        val fetched = CompletableDeferred<Unit>()
        var wroteToDatabase = false
        intents.receiveAsFlow().addFeedChanges { _, onSaving ->
            flow {
                withContext(NonCancellable) { fetched.await() }
                onSaving()
                wroteToDatabase = true
                emit(feed)
            }
        }.onEach(changes::add).launchIn(backgroundScope)

        intents.send(addIntent)
        runCurrent()
        intents.send(FeedIntent.CancelAddFeed)
        runCurrent()
        fetched.complete(Unit)
        runCurrent()

        assertFalse(wroteToDatabase)
        assertEquals(FeedPartialStateChange.AddFeed.Cancelled(url), changes.last())
        assertFalse(changes.any { it is FeedPartialStateChange.AddFeed.Failed })
    }

    @Test
    fun failureEndsLoadingAndAllowsAnotherAttempt() = runTest {
        val intents = Channel<FeedIntent>(Channel.UNLIMITED)
        val changes = mutableListOf<FeedPartialStateChange.AddFeed>()
        var attempts = 0
        intents.receiveAsFlow().addFeedChanges { _, onSaving ->
            flow {
                if (++attempts == 1) error("Network unavailable")
                onSaving()
                emit(feed)
            }
        }.onEach(changes::add).launchIn(backgroundScope)

        intents.send(addIntent)
        runCurrent()
        assertEquals(FeedPartialStateChange.AddFeed.Failed("Network unavailable"), changes.last())
        assertNull(changes.last().reduce(FeedState.initial().copy(addFeedState = AddFeedState.Loading)).addFeedState)
        intents.send(FeedIntent.CancelAddFeed)
        intents.send(addIntent)
        runCurrent()
        assertEquals(FeedPartialStateChange.AddFeed.Success(feed), changes.last())
    }
}
