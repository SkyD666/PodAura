package com.skyd.podaura.podnya.runtime

import com.skyd.podaura.podnya.capabilities.articles.articleSearchHistory
import com.skyd.podaura.podnya.core.AssistantAnswerKind
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantException
import com.skyd.podaura.podnya.core.AssistantHistoryTurn
import com.skyd.podaura.podnya.core.AssistantLimits
import com.skyd.podaura.podnya.core.AssistantTurnRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PodnyaMarkdownBudgetTest {
    @Test
    fun promptsKeepHistoryAt800AndSetIndependentTokenBudget() {
        val history = listOf(AssistantHistoryTurn(question = "old", reply = "h".repeat(800) + "EXCLUDED_TAIL"))
        val prompts = AssistantPrompts(
            request = AssistantTurnRequest(question = "new", history = history),
            capabilities = emptyList(),
        )
        val snapshot = prompts.snapshot(final = true)
        assertFalse(podnyaJson.encodeToString(snapshot).contains("EXCLUDED_TAIL"))
        assertTrue(podnyaJson.encodeToString(snapshot).contains("h".repeat(800)))
        assertEquals(800, articleSearchHistory(history).single().reply.length)
        assertEquals(AssistantLimits.OUTPUT_TOKENS, snapshot.params.maxTokens)
        val reply = prompts.reply(ValidatedAssistantReply(
            kind = AssistantAnswerKind.CLARIFICATION,
            text = "question",
            results = emptyMap(),
        ))
        assertEquals(16_384, reply.params.maxTokens)
        assertTrue(podnyaJson.encodeToString(reply).contains("8000 characters including Markdown markers"))
    }

    @Test
    fun structuredOutputBoundaryAccepts8000AndRejects8001() = runTest {
        val valid = AssistantOutput(kind = AssistantAnswerKind.CLARIFICATION, text = "x".repeat(8_000))
        assertEquals(valid.text, validateOutput(selection = valid, capabilities = emptyList()).text)
        assertEquals(AssistantError.INVALID_OUTPUT, assertFailsWith<AssistantException> {
            validateOutput(selection = valid.copy(text = valid.text + "x"), capabilities = emptyList())
        }.error)
    }

    @Test
    fun openAiEndpointsReceiveTokenParameterAndTruncatedStructuredJsonFails() = runTest {
        withContext(Dispatchers.Default) {
            val prompts = AssistantPrompts(request = AssistantTurnRequest("hello"), capabilities = emptyList())
            for (responses in listOf(false, true)) {
                val script = if (responses) HttpScript(responsesReply(responsesMessage("OK")), responsesStream())
                else HttpScript(chatCompletion("OK"), chatSse())
                PodnyaTransport(
                    connection = if (responses) astra() else testConnection(),
                    baseClient = script.client(),
                ).use { transport ->
                    val executor = BudgetedExecutor(transport.executor)
                    executor.execute(prompt = prompts.snapshot(final = true), model = transport.model, tools = emptyList())
                    collectReply(executor.executeStreaming(
                        prompt = prompts.reply(ValidatedAssistantReply(
                            kind = AssistantAnswerKind.CLARIFICATION,
                            text = "question",
                            results = emptyMap(),
                        )),
                        model = transport.model,
                        tools = emptyList(),
                    )) {}
                }
                val field = if (responses) "max_output_tokens" else "max_completion_tokens"
                script.requests.forEach { assertEquals(16_384, it[field]?.jsonPrimitive?.int) }
            }
            val truncated = HttpScript(HttpReply(chatCompletion("{}").body.replace("\"finish_reason\":\"stop\"", "\"finish_reason\":\"length\"")))
            PodnyaTransport(connection = testConnection(), baseClient = truncated.client()).use { transport ->
                val failure = assertFailsWith<AssistantException> {
                    BudgetedExecutor(transport.executor).execute(
                        prompt = prompts.snapshot(final = true),
                        model = transport.model,
                        tools = emptyList(),
                    )
                }
                assertEquals(AssistantError.INVALID_OUTPUT, failure.error)
            }
        }
    }
}
