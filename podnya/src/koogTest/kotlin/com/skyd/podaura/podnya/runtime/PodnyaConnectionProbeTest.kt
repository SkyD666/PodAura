package com.skyd.podaura.podnya.runtime

import com.skyd.podaura.podnya.core.AssistantReasoningEffort
import com.skyd.podaura.podnya.core.AssistantConnection
import com.skyd.podaura.podnya.core.AssistantError
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PodnyaConnectionProbeTest {
    @Test
    fun officialTerraProbeUsesResponsesForToolsStructuredOutputAndStreaming() = runTest {
        withContext(Dispatchers.Default) {
            for (effort in AssistantReasoningEffort.entries) {
                val connection = AssistantConnection(
                    baseUrl = "https://api.openai.com/v1/",
                    modelId = "gpt-6.1-terra",
                    apiKey = TEST_KEY,
                    reasoningEffort = effort,
                )
                val script = HttpScript(
                    responsesReply(responsesReasoning(), responsesCall("1", VALUE)),
                    responsesReply(responsesMessage("Received.")),
                    responsesReply(responsesMessage(VALUE)),
                    responsesStream(),
                )
                val result = PodnyaConnectionProbe(
                    connection = { connection },
                    httpClient = script::client,
                ).verify()
                assertTrue(result.success)
                assertEquals(4, script.requests.size)
                val expected = connection.timeouts
                script.timeouts.forEach { timeout ->
                    assertEquals(expected.connectMillis, timeout!!.connectTimeoutMillis)
                    assertEquals(expected.idleMillis, timeout.socketTimeoutMillis)
                }
                // Ktor SSE disables its request timer; collectReply and the outer turn bound it.
                script.timeouts.take(3).forEach { timeout ->
                    assertEquals(expected.requestMillis, timeout!!.requestTimeoutMillis)
                }
                assertTrue(script.paths.all { it == "/v1/responses" })
                script.requests.forEach {
                    assertEquals(JsonPrimitive(connection.modelId), it["model"])
                    assertEquals(effort.id, it.getValue("reasoning").jsonObject.getValue("effort").jsonPrimitive.content)
                }
                val toolResult = script.requests[1].getValue("input").jsonArray.map { it.jsonObject }
                    .single { it["type"] == JsonPrimitive("function_call_output") }
                assertEquals(JsonPrimitive("1"), toolResult["call_id"])
                assertEquals(JsonPrimitive(VALUE), toolResult["output"])
                assertEquals(JsonPrimitive(true), script.requests.last()["stream"])
            }
        }
    }

    @Test
    fun syntheticProbeRequiresRealToolResultStructuredOutputAndStreaming() = runTest {
        withContext(Dispatchers.Default) {
            val script = HttpScript(chatToolCalls("podnya_echo", "1" to VALUE), chatCompletion("Received."),
                chatCompletion(VALUE), chatSse())
            assertTrue(PodnyaConnectionProbe(::testConnection, script::client).verify().success)
            assertEquals(4, script.requests.size)
            assertTrue(script.paths.all { it == "/gateway/v1/chat/completions" })
            val result = script.requests[1].getValue("messages").jsonArray.map { it.jsonObject }
                .single { it["role"] == JsonPrimitive("tool") }
            assertEquals("1", result["tool_call_id"]!!.jsonPrimitive.content)
            assertEquals(VALUE, result["content"]!!.jsonPrimitive.content)
            assertTrue(script.requests[2].toString().contains("value"))
            assertEquals(JsonPrimitive(true), script.requests[3]["stream"])
            assertFalse(script.requests.any { it.toString().contains("search_articles") || it.toString().contains(TEST_KEY) })
            val unsupported = HttpScript(chatCompletion("I ran the test."))
            assertEquals(AssistantError.TOOLS_UNSUPPORTED,
                PodnyaConnectionProbe(::testConnection, unsupported::client).verify().error)
            assertEquals(1, unsupported.requests.size)
        }
    }

    @Test
    fun threeCallsAlwaysReachTheProviderAsToolResultsInBothProtocols() = runTest {
        withContext(Dispatchers.Default) {
            val reasoning = "private probe reasoning"
            val chat = HttpScript(chatToolCalls("podnya_echo", "1" to VALUE, "2" to VALUE, "3" to VALUE,
                reasoning = reasoning), chatCompletion("Received."), chatCompletion(VALUE), chatSse())
            val responses = HttpScript(responsesReply(responsesReasoning(), responsesCall("1", VALUE),
                responsesCall("2", VALUE), responsesCall("3", VALUE)), responsesReply(responsesMessage("Received.")),
                responsesReply(responsesMessage(VALUE)), responsesStream())
            assertTrue(PodnyaConnectionProbe(::testConnection, chat::client).verify().success)
            assertTrue(PodnyaConnectionProbe(::astra, responses::client).verify().success)
            val chatResults = chat.requests[1].getValue("messages").jsonArray.map { it.jsonObject }
                .filter { it["role"] == JsonPrimitive("tool") }
            assertEquals(listOf("1", "2", "3"), chatResults.map { it["tool_call_id"]!!.jsonPrimitive.content })
            val responseResults = responses.requests[1].getValue("input").jsonArray.map { it.jsonObject }
                .filter { it["type"] == JsonPrimitive("function_call_output") }
            assertEquals(listOf("1", "2", "3"), responseResults.map { it["call_id"]!!.jsonPrimitive.content })
            assertTrue(chat.requests[1].toString().contains(reasoning))
            assertTrue(responses.requests[1].toString().contains(ENCRYPTED))
            listOf(chat, responses).forEach { script ->
                assertEquals(4, script.requests.size)
                assertTrue(script.requests[1]["tools"] == null || script.requests[1]["tools"] == JsonArray(emptyList()))
                assertTrue(script.requests.drop(2).none { it.toString().contains(reasoning) || it.toString().contains(ENCRYPTED) })
            }
        }
    }

    @Test
    fun malformedAndUnknownCallsCountBeforeDecodeAndRepairStillFitsSevenRequests() = runTest {
        withContext(Dispatchers.Default) {
            val script = HttpScript(chatToolCalls("podnya_echo", "bad" to "{"),
                chatToolCalls("unknown", "unknown" to VALUE), chatToolCalls("podnya_echo", "good" to VALUE),
                chatCompletion("Received."), chatCompletion("not JSON"), chatCompletion(VALUE), chatSse())
            assertTrue(PodnyaConnectionProbe(::testConnection, script::client).verify().success)
            assertEquals(7, script.requests.size)
            assertTrue(script.requests[1].toString().contains("INVALID_ARGUMENT"))
            assertTrue(script.requests[2].toString().contains("UNKNOWN_TOOL"))
            assertTrue(script.requests[5].toString().contains("previous output was invalid"))
            val invalid = HttpScript(chatToolCalls("podnya_echo", "1" to "{", "2" to "{", "3" to "{"),
                chatCompletion("Received."))
            assertEquals(AssistantError.BUDGET_EXHAUSTED,
                PodnyaConnectionProbe(::testConnection, invalid::client).verify().error)
            assertEquals(2, invalid.requests.size)
        }
    }

    @Test
    fun excessCallsGetErrorsAndInvalidStructureGetsOnlyOneRepair() = runTest {
        withContext(Dispatchers.Default) {
            val script = HttpScript(chatToolCalls("podnya_echo", "1" to VALUE, "2" to VALUE, "3" to VALUE, "4" to VALUE),
                chatCompletion("Received."), chatCompletion("{}"), chatCompletion("{\"value\":\"wrong\"}"))
            val result = PodnyaConnectionProbe(::testConnection, script::client).verify()
            assertFalse(result.success)
            assertEquals(AssistantError.INVALID_OUTPUT, result.error)
            assertEquals(4, script.requests.size)
            val results = script.requests[1].getValue("messages").jsonArray.map { it.jsonObject }
                .filter { it["role"] == JsonPrimitive("tool") }
            assertEquals(4, results.size)
            assertTrue(results.last().toString().contains("TOOL_BUDGET_EXHAUSTED"))
        }
    }

    @Test
    fun probeCannotDiscardToolResultsWhenReasoningExceedsThePromptBudget() = runTest {
        withContext(Dispatchers.Default) {
            val largeReasoning = JsonObject(responsesReasoning() +
                ("encrypted_content" to JsonPrimitive("x".repeat(60 * 1024))))
            val script = HttpScript(responsesReply(largeReasoning, responsesCall("1", VALUE)))
            val result = PodnyaConnectionProbe(::astra, script::client).verify()
            assertFalse(result.success)
            assertEquals(AssistantError.BUDGET_EXHAUSTED, result.error)
            assertEquals(1, script.requests.size)
        }
    }

    @Test
    fun incompleteStreamingNeverPassesVerificationInEitherProtocol() = runTest {
        withContext(Dispatchers.Default) {
            val complete = chatSse()
            val chat = HttpScript(chatToolCalls("podnya_echo", "1" to VALUE), chatCompletion("Received."),
                chatCompletion(VALUE), complete.copy(body = complete.body.removeSuffix("data: [DONE]\n\n")))
            val responses = HttpScript(responsesReply(responsesCall("1", VALUE)), responsesReply(responsesMessage("Received.")),
                responsesReply(responsesMessage(VALUE)), responsesStream(terminal = false))
            assertEquals(AssistantError.STREAM_INTERRUPTED,
                PodnyaConnectionProbe(::testConnection, chat::client).verify().error)
            assertEquals(AssistantError.STREAM_INTERRUPTED,
                PodnyaConnectionProbe(::astra, responses::client).verify().error)
            assertEquals(4, chat.requests.size)
            assertEquals(4, responses.requests.size)
        }
    }

    @Test
    fun cancellationStopsProbeWithoutReturningAResultOrStartingAnotherRequest() = runTest {
        withContext(Dispatchers.Default) {
            val entered = CompletableDeferred<Unit>()
            val cancelled = CompletableDeferred<Unit>()
            var requests = 0
            var returned = false
            val probe = PodnyaConnectionProbe(::testConnection, {
                HttpClient(MockEngine {
                    requests++
                    entered.complete(Unit)
                    try { awaitCancellation() } finally { cancelled.complete(Unit) }
                })
            })
            val job = launch { probe.verify(); returned = true }
            withTimeout(5_000) { entered.await() }
            job.cancelAndJoin()
            withTimeout(5_000) { cancelled.await() }
            assertEquals(1, requests)
            assertFalse(returned)
        }
    }
}

private const val VALUE = "{\"value\":\"podnya-capability\"}"
