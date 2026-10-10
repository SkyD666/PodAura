package com.skyd.podaura.ui.notification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

class ArticleUpdateBatcherTest {
    @Test
    fun mergesAndDeduplicatesUntilTwentySecondsAfterTheLastUpdate() = runTest {
        val delivered = mutableListOf<List<String>>()
        val batcher = ArticleUpdateBatcher(backgroundScope) { delivered += it }
        batcher.send(listOf("first", "first"))
        runCurrent()
        advanceTimeBy(19_999)
        batcher.send(listOf("first", "second"))
        runCurrent()
        advanceTimeBy(19_999)
        assertTrue(delivered.isEmpty())
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(listOf("first", "second")), delivered)
    }

    @Test
    fun flushWaitsForSubmissionAndCancellingItsCallerDoesNotLoseLaterArticles() = runTest {
        val submitting = CompletableDeferred<Unit>()
        val submitted = CompletableDeferred<Unit>()
        val delivered = mutableListOf<List<String>>()
        val batcher = ArticleUpdateBatcher(backgroundScope) {
            if (it == listOf("first")) {
                submitting.complete(Unit)
                submitted.await()
            }
            delivered += it
        }
        batcher.send(listOf("first"))
        val flush = async { batcher.flush() }
        submitting.await()
        assertFalse(flush.isCompleted)
        batcher.send(listOf("second"))
        flush.cancelAndJoin()
        submitted.complete(Unit)
        batcher.flush()
        assertEquals(listOf(listOf("first"), listOf("second")), delivered)
    }

    @Test
    fun deliveryFailureDoesNotStopSubsequentBatchesOrFlushes() = runTest {
        val delivered = mutableListOf<List<String>>()
        val batcher = ArticleUpdateBatcher(backgroundScope) {
            if (it == listOf("failed")) error("Simulated notification service failure")
            delivered += it
        }
        val delivery = ArticleUpdateDelivery()
        withContext(delivery) { batcher.send(listOf("failed")) }
        assertFalse(batcher.flush(delivery))
        batcher.send(listOf("next"))
        batcher.flush()
        batcher.flush() // An empty lifecycle flush emits no notification.
        assertEquals(listOf(listOf("next")), delivered)
    }

    @Test
    fun lifecycleFlushCannotConsumeARefreshFailureOrContaminateAnotherRefresh() = runTest {
        val batcher = ArticleUpdateBatcher(backgroundScope) {
            if (it != listOf("successful")) error("Delivery failed")
        }
        batcher.setImmediateDelivery(true)
        val failed = ArticleUpdateDelivery()
        val successful = ArticleUpdateDelivery()
        withContext(failed) { launch { batcher.send(listOf("immediate")) } }
        runCurrent()
        batcher.flush() // The independent lifecycle flush cannot consume the failure.
        assertFalse(batcher.flush(failed))
        withContext(successful) {
            launch {
                withContext(NonCancellable) { batcher.send(listOf("successful")) }
            }
        }
        assertTrue(batcher.flush(successful))
        assertFalse(batcher.flush(failed))

        batcher.setImmediateDelivery(false)
        val timed = ArticleUpdateDelivery()
        withContext(timed) { batcher.send(listOf("timed")) }
        runCurrent()
        advanceTimeBy(20_000)
        runCurrent()
        batcher.flush()
        assertFalse(batcher.flush(timed))
    }

    @Test
    fun notificationFailureStillDownloadsAndIsReportedByFlush() = runTest {
        val downloaded = mutableListOf<String>()
        val batcher = ArticleUpdateBatcher(backgroundScope) { ids ->
            deliverArticleUpdates(
                ids,
                notify = { error("Notification failed") },
                download = { downloaded += it },
            )
        }
        val delivery = ArticleUpdateDelivery()
        withContext(delivery) { batcher.send(listOf("article")) }
        assertFalse(batcher.flush(delivery))
        assertEquals(listOf("article"), downloaded)
    }

    @Test
    fun bothDeliveryErrorsArePreserved() = runTest {
        val notificationFailure = IllegalStateException("Notification failed")
        val downloadFailure = IllegalStateException("Download failed")
        val failure = assertFailsWith<IllegalStateException> {
            deliverArticleUpdates(
                listOf("article"),
                notify = { throw notificationFailure },
                download = { throw downloadFailure },
            )
        }
        assertEquals(notificationFailure, failure)
        assertEquals(listOf(downloadFailure), failure.suppressedExceptions)
    }

    @Test
    fun suspensionFlushesPendingAndSubsequentArticlesAndForegroundRestoresBatching() = runTest {
        val delivered = mutableListOf<List<String>>()
        val batcher = ArticleUpdateBatcher(backgroundScope) { delivered += it }
        batcher.send(listOf("pending"))
        batcher.setImmediateDelivery(true)
        batcher.send(listOf("background"))
        runCurrent()
        assertEquals(listOf(listOf("pending"), listOf("background")), delivered)
        batcher.setImmediateDelivery(false)
        batcher.send(listOf("foreground"))
        runCurrent()
        assertEquals(2, delivered.size)
        batcher.flush()
        assertEquals(listOf("foreground"), delivered.last())
    }
}
