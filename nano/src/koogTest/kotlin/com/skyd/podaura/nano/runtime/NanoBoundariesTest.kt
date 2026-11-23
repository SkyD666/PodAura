package com.skyd.podaura.nano.runtime

import com.skyd.podaura.nano.runtime.client.nanoModel
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.streaming.StreamFrame
import com.skyd.podaura.nano.core.AssistantError
import com.skyd.podaura.nano.core.AssistantException
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KLoggerFactory
import io.github.oshai.kotlinlogging.KLoggingEventBuilder
import io.github.oshai.kotlinlogging.Level
import io.github.oshai.kotlinlogging.Marker
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame

class NanoBoundariesTest {
    @Test
    fun loggerFilterNeverEvaluatesKoogOrUnnamedPayloadsAndDelegatesOtherNamespaces() {
        val ordinary = object : KLogger {
            override val name = "application"
            override fun isLoggingEnabledFor(level: Level, marker: Marker?) = true
            override fun at(level: Level, marker: Marker?, block: KLoggingEventBuilder.() -> Unit) = Unit
        }
        val factory = NanoLoggerFactory(object : KLoggerFactory {
            override fun logger(name: String): KLogger = ordinary
        })
        var evaluated = false
        listOf("ai.koog.agents.core.agent.GraphAIAgent", "ai.koog.prompt.OpenAILLMClient", "", "UnknownLogger")
            .forEach { factory.logger(it).error { evaluated = true; "sensitive payload" } }
        assertFalse(evaluated)
        assertSame(ordinary, factory.logger("com.skyd.podaura"))
        assertSame(ordinary, factory.logger("ai.koogish.unrelated"))
    }

    @Test
    fun sevenCallBudgetIncludesStreamingAndRejectsOversizedPromptsBeforeExecution() = runTest {
        var actualCalls = 0
        val delegate = object : PromptExecutor() {
            override suspend fun execute(prompt: Prompt, model: LLModel, tools: List<ToolDescriptor>): Message.Assistant {
                actualCalls++
                return Message.Assistant("ok", ResponseMetaInfo.Empty, "stop")
            }
            override fun executeStreaming(prompt: Prompt, model: LLModel, tools: List<ToolDescriptor>) =
                flowOf(StreamFrame.TextDelta("ok"), StreamFrame.TextComplete("ok"), StreamFrame.End("stop"))
                    .also { actualCalls++ }
            override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult = error("unused")
            override fun close() = Unit
        }
        val executor = BudgetedExecutor(delegate)
        val model = nanoModel("custom")
        val oversized = assertFailsWith<AssistantException> {
            executor.execute(prompt("huge") { user("中".repeat(25_000)) }, model, emptyList())
        }
        assertEquals(AssistantError.BUDGET_EXHAUSTED, oversized.error)
        assertEquals(0, actualCalls)
        repeat(6) { executor.execute(prompt("small") { user("test") }, model, emptyList()) }
        executor.executeStreaming(prompt("stream") { user("test") }, model, emptyList()).toList()
        assertEquals(7, executor.calls)
        assertEquals(7, actualCalls)
        assertEquals(AssistantError.BUDGET_EXHAUSTED, assertFailsWith<AssistantException> {
            executor.execute(prompt("eighth") { user("test") }, model, emptyList())
        }.error)
        assertEquals(7, actualCalls)
    }
}
