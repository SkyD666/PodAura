package com.skyd.podaura.podnya.runtime

import com.skyd.podaura.podnya.core.AssistantContent
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantException
import com.skyd.podaura.podnya.core.AssistantLimits
import com.skyd.podaura.podnya.core.AssistantPhase
import com.skyd.podaura.podnya.core.AssistantRunEvent
import com.skyd.podaura.podnya.core.AssistantTurnRequest
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AssistantRunTest {
    @Test
    fun textOnlyCapabilityGetsProgressDeltasAndOneCompletion() = runTest {
        val events = assistantRunEvents(AssistantTurnRequest("Say hello")) { event ->
            event(AssistantRunEvent.ReplyDelta("Hello"))
        }.toList()
        assertEquals(listOf(
            AssistantRunEvent.Progress(AssistantPhase.PREPARING),
            AssistantRunEvent.ReplyDelta("Hello"),
            AssistantRunEvent.Completed,
        ), events)
    }

    @Test
    fun invalidQuestionsNeverEnterCapabilityCode() = runTest {
        for (question in listOf(" ", "x".repeat(AssistantLimits.INPUT_CHARS + 1))) {
            var entered = false
            val events = assistantRunEvents(AssistantTurnRequest(question)) { entered = true }.toList()
            assertFalse(entered)
            assertEquals(AssistantRunEvent.Failed(AssistantError.INVALID_OUTPUT), events.last())
            assertFalse(events.contains(AssistantRunEvent.Completed))
        }
    }

    @Test
    fun errorsKeepTheirCodeBeforeOutputAndReportInterruptionAfterTextOrContent() = runTest {
        val outputs = listOf(null, AssistantRunEvent.ReplyDelta("Partial"), AssistantRunEvent.ContentReady(SampleContent))
        for (output in outputs) {
            val events = assistantRunEvents(AssistantTurnRequest("Question")) { event ->
                output?.let { event(it) }
                throw AssistantException(AssistantError.AUTHENTICATION)
            }.toList()
            assertEquals(AssistantRunEvent.Failed(
                if (output == null) AssistantError.AUTHENTICATION else AssistantError.STREAM_INTERRUPTED,
            ), events.last())
            assertFalse(events.contains(AssistantRunEvent.Completed))
        }
    }

    @Test
    fun timeoutDistinguishesNoOutputFromAnInterruptedReply() = runTest {
        for (published in listOf(false, true)) {
            val events = assistantRunEvents(AssistantTurnRequest("Question")) { event ->
                if (published) event(AssistantRunEvent.ReplyDelta("Partial"))
                delay(AssistantLimits.TURN_TIMEOUT_MILLIS + 1)
            }.toList()
            assertEquals(AssistantRunEvent.Failed(
                if (published) AssistantError.STREAM_INTERRUPTED else AssistantError.TIMEOUT,
            ), events.last())
            assertFalse(events.contains(AssistantRunEvent.Completed))
        }
    }

    @Test
    fun cancellationReachesCapabilityWithoutBecomingAFailureEvent() = runTest {
        var cancelled = false
        val events = mutableListOf<AssistantRunEvent>()
        val job = launch {
            assistantRunEvents(AssistantTurnRequest("Question")) { event ->
                event(AssistantRunEvent.ReplyDelta("Partial"))
                try { awaitCancellation() } finally { cancelled = true }
            }.toList(events)
        }
        runCurrent()
        job.cancelAndJoin()
        assertEquals(true, cancelled)
        assertEquals(listOf(
            AssistantRunEvent.Progress(AssistantPhase.PREPARING), AssistantRunEvent.ReplyDelta("Partial"),
        ), events)
    }

    private object SampleContent : AssistantContent {
        override val retainedBytes = 0
        override fun historySnapshot() = null
    }
}
