package com.skyd.podaura.nano.runtime

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.streaming.StreamFrame
import com.skyd.podaura.nano.core.AssistantConnection
import com.skyd.podaura.nano.core.AssistantError
import com.skyd.podaura.nano.core.AssistantProtocol
import com.skyd.podaura.nano.core.AssistantReasoningEffort
import com.skyd.podaura.nano.core.AssistantRunEvent
import com.skyd.podaura.nano.core.AssistantTimeouts
import com.skyd.podaura.nano.core.AssistantTurnRequest
import com.skyd.podaura.nano.runtime.client.nanoModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NanoReasoningTimeoutTest {
    private val request = AssistantTurnRequest("Question")
    private val model = nanoModel("gpt-6.1-terra")
    private val prompt = prompt("test") { user("Question") }

    private fun timeouts(effort: AssistantReasoningEffort) = AssistantConnection(
        baseUrl = "https://api.openai.com/v1/",
        modelId = model.id,
        apiKey = TEST_KEY,
        reasoningEffort = effort,
    ).timeouts

    @Test
    fun probeConfigurationResolutionRemainsBoundedBeforeCreatingAClient() = runTest {
        var cancelled = false
        val result = NanoConnectionProbe(
            connection = {
                try {
                    awaitCancellation()
                } finally {
                    cancelled = true
                }
            },
            httpClient = { error("Must not create a client before resolving the connection") },
        ).verify()
        assertEquals(90_000, currentTime)
        assertEquals(AssistantError.TIMEOUT, result.error)
        assertTrue(cancelled)
    }

    @Test
    fun higherEffortsAllowSlowToolsAndSilentReasoningBeforeFinalText() = runTest {
        for (effort in AssistantReasoningEffort.entries.filter { it != AssistantReasoningEffort.LOW }) {
            val timeouts = timeouts(effort)
            val executor = BudgetedExecutor(delegate = SlowExecutor(100_000), timeouts = timeouts)
            val events = assistantRunEvents(request = request, timeouts = timeouts) { event ->
                repeat(2) {
                    executor.execute(prompt = prompt, model = model, tools = emptyList())
                }
                collectReply(
                    frames = executor.executeStreaming(prompt = prompt, model = model, tools = emptyList()),
                    timeouts = timeouts,
                ) { event(AssistantRunEvent.ReplyDelta(it)) }
            }.toList()
            assertEquals(AssistantRunEvent.Completed, events.last())
            assertEquals(listOf("OK"), events.filterIsInstance<AssistantRunEvent.ReplyDelta>().map { it.text })
        }
    }

    @Test
    fun compatibleGatewaysDoNotInheritATimeoutFromAModelNameOrUnsupportedEffort() {
        for (baseUrl in listOf("https://gateway.example/v1/", "https://generativelanguage.googleapis.com.example/v1/")) {
            for (modelId in listOf(model.id, "gemini-3.8-flash", "new-model")) {
                val connection = AssistantConnection(
                    baseUrl = baseUrl,
                    modelId = modelId,
                    apiKey = TEST_KEY,
                    reasoningEffort = AssistantReasoningEffort.MAX,
                )
                assertEquals(AssistantTimeouts.STANDARD, connection.timeouts)
            }
        }
    }

    @Test
    fun geminiCanFinishASlowRequestAndThenWaitForTheFirstStreamedText() = runTest {
        val connections = listOf(
            AssistantConnection(
                baseUrl = "https://example.test/v1beta/",
                modelId = "custom-model",
                apiKey = TEST_KEY,
                protocol = AssistantProtocol.GEMINI_GENERATE_CONTENT,
            ),
            AssistantConnection(
                baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai/",
                modelId = "gemini-3.8-flash",
                apiKey = TEST_KEY,
            ),
        )
        for (connection in connections) {
            val timeouts = connection.timeouts
            val executor = BudgetedExecutor(delegate = SlowExecutor(55_000), timeouts = timeouts)
            val events = assistantRunEvents(request = request, timeouts = timeouts) { event ->
                executor.execute(prompt = prompt, model = model, tools = emptyList())
                collectReply(
                    frames = executor.executeStreaming(prompt = prompt, model = model, tools = emptyList()),
                    timeouts = timeouts,
                ) { event(AssistantRunEvent.ReplyDelta(it)) }
            }.toList()
            assertEquals(AssistantRunEvent.Completed, events.last())
            assertTrue(AssistantRunEvent.ReplyDelta("OK") in events)
        }
    }

    @Test
    fun successiveCallsShareOneTenMinuteDeadline() = runTest {
        val timeouts = timeouts(AssistantReasoningEffort.MAX)
        val delegate = SlowExecutor(250_000)
        val executor = BudgetedExecutor(delegate = delegate, timeouts = timeouts)
        val start = currentTime
        val events = assistantRunEvents(request = request, timeouts = timeouts) {
            repeat(3) {
                executor.execute(prompt = prompt, model = model, tools = emptyList())
            }
        }.toList()
        assertEquals(600_000, currentTime - start)
        assertEquals(AssistantRunEvent.Failed(AssistantError.TIMEOUT), events.last())
        assertTrue(delegate.interrupted)
        assertFalse(events.contains(AssistantRunEvent.Completed))
    }

    @Test
    fun cancellationInterruptsHighEffortImmediatelyWithoutPublishingFailure() = runTest {
        val timeouts = timeouts(AssistantReasoningEffort.HIGH)
        val delegate = SlowExecutor(250_000)
        val executor = BudgetedExecutor(delegate = delegate, timeouts = timeouts)
        val events = mutableListOf<AssistantRunEvent>()
        val job = launch {
            assistantRunEvents(request = request, timeouts = timeouts) { event ->
                collectReply(
                    frames = executor.executeStreaming(prompt = prompt, model = model, tools = emptyList()),
                    timeouts = timeouts,
                ) { event(AssistantRunEvent.ReplyDelta(it)) }
            }.toList(events)
        }
        runCurrent()
        advanceTimeBy(20_000)
        job.cancelAndJoin()
        assertTrue(delegate.interrupted)
        assertFalse(events.any { it is AssistantRunEvent.Failed || it == AssistantRunEvent.Completed })
    }

    @Test
    fun lowEffortStillUsesTheExistingRequestTimeout() = runTest {
        val timeouts = timeouts(AssistantReasoningEffort.LOW)
        val executor = BudgetedExecutor(delegate = SlowExecutor(100_000), timeouts = timeouts)
        val events = assistantRunEvents(request = request, timeouts = timeouts) {
            executor.execute(prompt = prompt, model = model, tools = emptyList())
        }.toList()
        assertEquals(45_000, currentTime)
        assertEquals(AssistantRunEvent.Failed(AssistantError.TIMEOUT), events.last())
    }

    private class SlowExecutor(private val waitMillis: Long) : PromptExecutor() {
        var interrupted = false

        private suspend fun waitForModel() {
            var completed = false
            try {
                delay(waitMillis)
                completed = true
            } finally {
                if (!completed) interrupted = true
            }
        }

        override suspend fun execute(prompt: Prompt, model: LLModel, tools: List<ToolDescriptor>): Message.Assistant {
            waitForModel()
            return Message.Assistant(content = "OK", metaInfo = ResponseMetaInfo.Empty, finishReason = "stop")
        }

        override fun executeStreaming(prompt: Prompt, model: LLModel, tools: List<ToolDescriptor>) = flow {
            emit(StreamFrame.ReasoningDelta(text = "private"))
            waitForModel()
            emit(StreamFrame.TextDelta("OK"))
            emit(StreamFrame.TextComplete("OK"))
            emit(StreamFrame.End("stop"))
        }

        override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult = error("unused")
        override fun close() = Unit
    }
}
