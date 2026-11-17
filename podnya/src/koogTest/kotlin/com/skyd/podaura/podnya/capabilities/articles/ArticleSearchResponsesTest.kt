package com.skyd.podaura.podnya.capabilities.articles

import com.skyd.podaura.podnya.capabilities.articles.model.repository.LocalArticleSearchRepository
import com.skyd.podaura.podnya.runtime.emptyFeedSource
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantRunEvent
import com.skyd.podaura.podnya.core.AssistantTurnRequest
import com.skyd.podaura.podnya.runtime.ENCRYPTED
import com.skyd.podaura.podnya.runtime.HttpReply
import com.skyd.podaura.podnya.runtime.HttpScript
import com.skyd.podaura.podnya.runtime.testCapabilities
import com.skyd.podaura.podnya.runtime.KoogAssistantRuntime
import com.skyd.podaura.podnya.runtime.TEST_KEY
import com.skyd.podaura.podnya.runtime.astra
import com.skyd.podaura.podnya.runtime.podnyaJson
import com.skyd.podaura.podnya.runtime.responsesCall
import com.skyd.podaura.podnya.runtime.responsesMessage as message
import com.skyd.podaura.podnya.runtime.responsesObject as responseObject
import com.skyd.podaura.podnya.runtime.responsesReasoning as reasoning
import com.skyd.podaura.podnya.runtime.responsesReply as response
import com.skyd.podaura.podnya.runtime.responsesStream as stream
import com.skyd.podaura.podnya.core.AssistantConnection
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class ArticleSearchResponsesTest {
    @Test
    fun officialAstraPreservesToolExchangeAndPhaseWhileStreamingOnlyReplyText() =
        verifyToolExchangeAndStreaming(astra())

    @Test
    fun officialTerraPreservesToolExchangeAndPhaseWhileStreamingOnlyReplyText() =
        verifyToolExchangeAndStreaming(
            AssistantConnection(
                baseUrl = "https://api.openai.com/v1/",
                modelId = "gpt-6.1-terra",
                apiKey = TEST_KEY,
            )
        )

    private fun verifyToolExchangeAndStreaming(connection: AssistantConnection) = runTest {
        withContext(Dispatchers.Default) {
            val original = listOf(reasoning(), message("I will search.", "commentary"), call("s1"))
            val misleading = "Search is closed; the candidates have not been verified."
            val script = HttpScript(response(*original.toTypedArray()), response(message("Search finished.")),
                selection(text = misleading), stream())
            val catalog = RecordingCatalog()
            val events = script.assistant(catalog = catalog, connection = connection)
                .run(AssistantTurnRequest("Find articles"), connection).toList()
            assertEquals(AssistantRunEvent.Completed, events.last())
            assertEquals(4, script.requests.size)
            assertTrue(script.paths.all { it == "/v1/responses" })
            script.requests.forEach {
                assertEquals(JsonPrimitive(connection.modelId), it["model"])
                assertEquals(JsonPrimitive(false), it["store"])
                assertEquals("low", it["reasoning"]!!.jsonObject["effort"]!!.jsonPrimitive.content)
                assertTrue("reasoning.encrypted_content" in it["include"].toString())
                assertFalse(it.containsKey("temperature") || it.containsKey("top_p") || it.containsKey("previous_response_id"))
                (it["tools"] as? JsonArray)?.forEach { tool -> assertEquals(JsonPrimitive(false), tool.jsonObject["strict"]) }
            }
            val secondInput = script.requests[1]["input"]!!.jsonArray
            val replayStart = secondInput.indexOfFirst { (it as? JsonObject)?.get("id") == JsonPrimitive("rs-test") }
            assertEquals(original, secondInput.subList(replayStart, replayStart + original.size))
            assertTrue(secondInput.any { (it as? JsonObject)?.get("type") == JsonPrimitive("function_call_output") &&
                it["call_id"] == JsonPrimitive("s1") })
            script.requests.drop(2).forEach { assertFalse(it.toString().contains(ENCRYPTED)) }
            val selectionInput = script.requests[2].getValue("input").jsonArray.map { it.jsonObject }
            val instructions = selectionInput.filter { it["role"] == JsonPrimitive("developer") }.joinToString()
            assertTrue(instructions.contains("Select and rank local articles already retrieved"))
            assertFalse(instructions.contains("Use search_articles"))
            assertFalse(selectionInput.toString().contains("Search is closed"))
            val replyInput = script.requests.last().getValue("input").jsonArray.last().jsonObject
                .getValue("content").jsonArray.single().jsonObject.getValue("text").jsonPrimitive.content
                .let { podnyaJson.parseToJsonElement(it).jsonObject }
            assertEquals(JsonPrimitive(1), replyInput.getValue("outputs").jsonObject.getValue("articles").jsonObject["successfulSearches"])
            assertFalse(script.requests.last().toString().contains(misleading))
            assertFalse(events.toString().contains(ENCRYPTED) || events.toString().contains("hidden reasoning"))
            assertFalse(events.toString().contains(misleading))
            assertEquals("Found one.", events.filterIsInstance<AssistantRunEvent.ReplyDelta>().joinToString("") { it.text })
            assertEquals(2, catalog.queries.size) // Search plus final recheck.
        }
    }

    @Test
    fun rawMalformedArgumentsConsumeTheSameToolBudgetBeforeParsing() = runTest {
        withContext(Dispatchers.Default) {
            val script = HttpScript(response(call("bad", "not JSON"), call("good"), call("third"), call("excess")),
                selection(), stream())
            val catalog = RecordingCatalog()
            val events = script.assistant(catalog, astra()).run(AssistantTurnRequest("Find articles"), com.skyd.podaura.podnya.runtime.astra()).toList()
            assertEquals(AssistantRunEvent.Completed, events.last())
            assertEquals(3, catalog.queries.size) // Two searches plus final recheck.
            assertEquals(3, script.requests.size)
        }
    }

    @Test
    fun incompleteOrFailedNonstreamResponsesNeverExecuteTools() = runTest {
        withContext(Dispatchers.Default) {
            val valid = responseObject(call("1"))
            val invalid = listOf(
                valid + ("status" to JsonPrimitive("incomplete")),
                valid + ("status" to JsonPrimitive("failed")),
                valid + ("error" to buildJsonObject { put("code", "server_error"); put("message", TEST_KEY) }),
                valid + ("incomplete_details" to buildJsonObject { put("reason", "max_output_tokens") }),
            )
            invalid.forEach { body ->
                val catalog = RecordingCatalog()
                val script = HttpScript(HttpReply(JsonObject(body).toString()))
                val events = script.assistant(catalog, astra()).run(AssistantTurnRequest("Find articles"), com.skyd.podaura.podnya.runtime.astra()).toList()
                assertEquals(AssistantRunEvent.Failed(AssistantError.INVALID_OUTPUT), events.last())
                assertTrue(catalog.queries.isEmpty())
                assertEquals(1, script.requests.size)
                assertFalse(events.toString().contains(TEST_KEY))
            }
        }
    }

    @Test
    fun missingDuplicateFailedOrMismatchedStreamCompletionPreservesCardsButFails() = runTest {
        withContext(Dispatchers.Default) {
            val invalidStreams = listOf(
                stream(terminal = false),
                stream(duplicate = true),
                stream(terminalType = "response.failed"),
                stream(terminalType = "response.incomplete"),
                stream(status = "incomplete"),
                stream(finalText = "Different final text."),
                HttpReply(stream().body + "data: [DONE]\n\n", sse = true),
            )
            invalidStreams.forEach { sse ->
                val script = HttpScript(response(call("1")), response(message("Search finished.")), selection(), sse)
                val events = script.assistant(RecordingCatalog(), astra()).run(AssistantTurnRequest("Find articles"), com.skyd.podaura.podnya.runtime.astra()).toList()
                assertTrue(events.any { it is AssistantRunEvent.ContentReady })
                assertFalse(events.contains(AssistantRunEvent.Completed))
                assertEquals(AssistantError.STREAM_INTERRUPTED, (events.last() as AssistantRunEvent.Failed).error)
                assertEquals(4, script.requests.size)
            }
        }
    }

    @Test
    fun replayedProviderOutputCannotBypassTheWireBudget() = runTest {
        withContext(Dispatchers.Default) {
            // SDK conversion omits this field; exact item replay must still respect the byte limit.
            val large = JsonObject(call("1") + ("provider_metadata" to JsonPrimitive("x".repeat(65_536))))
            val script = HttpScript(response(large))
            val catalog = RecordingCatalog()
            val events = script.assistant(catalog, astra()).run(AssistantTurnRequest("Find articles"), com.skyd.podaura.podnya.runtime.astra()).toList()
            assertEquals(AssistantRunEvent.Failed(AssistantError.BUDGET_EXHAUSTED), events.last())
            assertEquals(1, script.requests.size)
            assertEquals(1, catalog.queries.size)
        }
    }

    @Test
    fun cancellationStopsResponsesWithoutStartingAnotherRequest() = runTest {
        withContext(Dispatchers.Default) {
            val entered = CompletableDeferred<Unit>()
            var requests = 0
            val assistant = KoogAssistantRuntime(testCapabilities(LocalArticleSearchRepository(RecordingCatalog()), emptyFeedSource()), { astra() }, {
                HttpClient(MockEngine {
                    requests++
                    entered.complete(Unit)
                    awaitCancellation()
                })
            })
            val events = mutableListOf<AssistantRunEvent>()
            val job = launch { assistant.run(AssistantTurnRequest("Find articles"), com.skyd.podaura.podnya.runtime.astra()).toList(events) }
            withTimeout(5_000) { entered.await() }
            job.cancelAndJoin()
            assertEquals(1, requests)
            assertFalse(events.any { it is AssistantRunEvent.Failed || it is AssistantRunEvent.Completed })
        }
    }
}

private fun call(id: String, arguments: String = "{}") = responsesCall(id, arguments, "search_articles")

private fun selection(id: String = "a", text: String = "Found a matching local article.") = response(message(buildJsonObject {
    put("kind", "RESULTS"); put("text", text)
    put("outputs", buildJsonObject { put("articles", buildJsonObject {
        put("articles", buildJsonArray { add(buildJsonObject { put("articleId", id); put("reason", "Matches the request.") }) })
    }) })
}.toString()))

