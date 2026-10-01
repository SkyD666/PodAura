package com.skyd.podaura.model.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BatchProgressTest {
    @Test
    fun failuresAndSkippedItemsDoNotStopLaterItems() = runTest {
        val visited = mutableListOf<Int>()
        val progress = processBatch(listOf(1, 2, 3, 4)) {
            visited += it
            check(it != 2)
            it != 3
        }.toList()
        assertEquals(listOf(1, 2, 3, 4), visited)
        assertEquals(BatchProgress(4, successCount = 2, skippedCount = 1, failedCount = 1), progress.last())
        assertEquals(listOf(4, 3, 2, 1, 0), progress.map { it.remainingCount })
    }

    @Test
    fun cancellationPreservesCompletedProgressAndStopsRemainingItems() = runTest {
        val visited = mutableListOf<Int>()
        val progress = mutableListOf<BatchProgress>()
        assertFailsWith<CancellationException> {
            processBatch(listOf(1, 2, 3)) {
                visited += it
                if (it == 2) throw CancellationException()
                true
            }.collect { progress += it }
        }
        assertEquals(listOf(1, 2), visited)
        assertEquals(BatchProgress(3, successCount = 1), progress.last())
    }
}
