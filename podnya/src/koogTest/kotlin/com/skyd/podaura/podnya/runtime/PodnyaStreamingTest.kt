package com.skyd.podaura.podnya.runtime

import com.skyd.podaura.podnya.core.AssistantFailure
import ai.koog.prompt.streaming.StreamFrame
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PodnyaStreamingTest {
    @Test
    fun emitsOnlyTextDeltasAndRequiresCompleteAndNormalEnd() = runTest {
        val deltas = mutableListOf<String>()
        collectReply(flowOf(StreamFrame.ReasoningDelta(text = "hidden"),
            StreamFrame.TextDelta("one "), StreamFrame.TextDelta("two"),
            StreamFrame.TextComplete("one two"), StreamFrame.End("stop"))) { deltas += it }
        assertEquals(listOf("one ", "two"), deltas)
        val valid = listOf(StreamFrame.TextDelta("text"), StreamFrame.TextComplete("text"), StreamFrame.End("stop"))
        val invalid = listOf(
            valid.dropLast(1), valid + StreamFrame.End("stop"),
            listOf(StreamFrame.TextComplete("text"), StreamFrame.End("stop")),
            valid.dropLast(1) + StreamFrame.End("length"),
            valid.dropLast(1) + StreamFrame.End(null),
            listOf(valid[0], valid[1], valid[1], valid[2]),
            listOf(valid[0], StreamFrame.TextComplete("other"), valid[2]),
            valid + StreamFrame.TextDelta("late"),
        )
        invalid.forEach { frames ->
            val failure = assertFailsWith<AssistantException> { collectReply(flowOf(*frames.toTypedArray())) {} }
            assertEquals(AssistantError.STREAM_INTERRUPTED, failure.error)
        }
    }

    @Test
    fun overlongTextRetainsTheBoundedPrefixAndSignalsInterruption() = runTest {
        var text = ""
        assertFailsWith<AssistantException> {
            collectReply(flowOf(StreamFrame.TextDelta("x".repeat(900)))) { text += it }
        }
        assertEquals(800, text.length)
    }

    @Test
    fun firstTextAndIdleTimeoutsAreFailuresWhileUserCancellationPropagates() = runTest {
        var failure: AssistantFailure? = null
        val stalled = launch {
            try {
                collectReply(flow { emit(StreamFrame.ReasoningDelta(text = "hidden")); awaitCancellation() }) {}
            } catch (error: AssistantException) { failure = error.error }
        }
        advanceTimeBy(15_001)
        runCurrent()
        stalled.join()
        assertEquals(AssistantError.STREAM_INTERRUPTED, failure)

        var cancellation = false
        var converted = false
        val stopped = launch {
            try { collectReply(flow { emit(StreamFrame.TextDelta("partial")); awaitCancellation() }) {} }
            catch (_: CancellationException) { cancellation = true }
            catch (_: AssistantException) { converted = true }
        }
        runCurrent()
        stopped.cancelAndJoin()
        assertTrue(cancellation)
        assertFalse(converted)
    }
}
