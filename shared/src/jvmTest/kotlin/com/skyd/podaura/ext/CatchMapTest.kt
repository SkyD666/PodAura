package com.skyd.podaura.ext

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

class CatchMapTest {
    @Test
    fun logsOffCollectorThreadAndPreservesRecoveryAndCancellation() = runBlocking<Unit> {
        val collectorThread = Thread.currentThread()
        var logThread: Thread? = null
        val failure = object : IllegalStateException("bad media index") {
            override fun printStackTrace() {
                logThread = Thread.currentThread()
            }
        }
        val values = flow {
            emit("loading")
            throw failure
        }.catchMap {
            assertSame(failure, it)
            assertSame(collectorThread, Thread.currentThread())
            "failed"
        }.toList()

        assertEquals(listOf("loading", "failed"), values)
        checkNotNull(logThread)
        assertNotEquals(collectorThread, logThread)

        assertFailsWith<TimeoutCancellationException> {
            withTimeout(50) {
                flow<String> { awaitCancellation() }
                    .catchMap { error("Cancellation must not become a failure state") }
                    .toList()
            }
        }
    }
}
