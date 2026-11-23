package com.skyd.podaura.nano.runtime

import com.skyd.podaura.nano.capabilities.articles.articleSearchHistory
import com.skyd.podaura.nano.core.AssistantAnswerKind
import com.skyd.podaura.nano.core.AssistantError
import com.skyd.podaura.nano.core.AssistantException
import com.skyd.podaura.nano.core.AssistantHistoryTurn
import com.skyd.podaura.nano.core.AssistantLimits
import com.skyd.podaura.nano.core.AssistantTurnRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NanoMarkdownBudgetTest {
    @Test
    fun promptsKeepHistoryAt800AndSetIndependentTokenBudget() {
        val history = listOf(AssistantHistoryTurn(question = "old", reply = "h".repeat(800) + "EXCLUDED_TAIL"))
        val prompts = AssistantPrompts(
            request = AssistantTurnRequest(question = "new", history = history),
            capabilities = emptyList(),
        )
        val snapshot = prompts.snapshot(final = true)
        assertFalse(nanoJson.encodeToString(snapshot).contains("EXCLUDED_TAIL"))
        assertTrue(nanoJson.encodeToString(snapshot).contains("h".repeat(800)))
        assertEquals(800, articleSearchHistory(history).single().reply.length)
        assertEquals(AssistantLimits.OUTPUT_TOKENS, snapshot.params.maxTokens)
        val reply = prompts.reply(ValidatedAssistantReply(
            kind = AssistantAnswerKind.CLARIFICATION,
            text = "question",
            results = emptyMap(),
        ))
        assertEquals(16_384, reply.params.maxTokens)
        assertTrue(nanoJson.encodeToString(reply).contains("8000 characters including Markdown markers"))
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
    fun directExplanationCannotPublishResultsOrInvalidText() {
        val valid = AssistantOutput(kind = AssistantAnswerKind.UNSUPPORTED, text = "x".repeat(AssistantLimits.REPLY_CHARS))
        assertEquals(valid.text, parseDirectExplanation(nanoJson.encodeToString(valid))?.text)
        for (invalid in listOf(
            valid.copy(text = " "),
            valid.copy(text = valid.text + "x"),
            valid.copy(kind = AssistantAnswerKind.RESULTS),
            valid.copy(kind = AssistantAnswerKind.NO_MATCH),
            valid.copy(outputs = mapOf("articles" to JsonObject(emptyMap()))),
        )) {
            assertNull(parseDirectExplanation(nanoJson.encodeToString(invalid)))
        }
    }

    @Test
    fun openAiEndpointsReceiveTokenParameterAndTruncatedStructuredJsonFails() = runTest {
        withContext(Dispatchers.Default) {
            val prompts = AssistantPrompts(request = AssistantTurnRequest("hello"), capabilities = emptyList())
            for (responses in listOf(false, true)) {
                val script = if (responses) HttpScript(responsesReply(responsesMessage("OK")), responsesStream())
                else HttpScript(chatCompletion("OK"), chatSse())
                NanoTransport(
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
            NanoTransport(connection = testConnection(), baseClient = truncated.client()).use { transport ->
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
