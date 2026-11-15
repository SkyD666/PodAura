package com.skyd.podaura.podnya.runtime

import ai.koog.prompt.dsl.prompt
import com.skyd.podaura.podnya.capabilities.articles.RecordingCatalog
import com.skyd.podaura.podnya.capabilities.articles.model.repository.LocalArticleSearchRepository
import com.skyd.podaura.podnya.core.AssistantRunEvent
import com.skyd.podaura.podnya.core.AssistantTurnRequest
import com.skyd.podaura.podnya.core.assistantEndpointPath
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.toList
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantException
import com.skyd.podaura.podnya.core.AssistantConnection
import com.skyd.podaura.podnya.core.AssistantProtocol
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PodnyaNativeProtocolTest {
    @Test
    fun nativeRunsUseIndependentFixedConnectionsAndTheSameTransportAsProbes() = nativeTest {
        val clarification = """{"kind":"CLARIFICATION","text":"What topic?","outputs":{}}"""
        val scripts = listOf(
            NativeScript(
                protocol = AssistantProtocol.ANTHROPIC_MESSAGES,
                replies = listOf(HttpReply(claudeText("Need detail")), HttpReply(claudeText(clarification)), HttpReply(body = claudeStream(), sse = true)),
            ),
            NativeScript(
                protocol = AssistantProtocol.GEMINI_GENERATE_CONTENT,
                replies = listOf(HttpReply(geminiText("Need detail")), HttpReply(geminiText(clarification)), HttpReply(body = geminiStream(), sse = true)),
            ),
        )
        coroutineScope {
            scripts.map { script -> async {
                val runtime = KoogAssistantRuntime(
                    capabilities = testCapabilities(LocalArticleSearchRepository(RecordingCatalog()), emptyFeedSource()),
                    verificationConnection = { error("Never reread global configuration during a run") },
                    httpClient = script::client,
                )
                val events = runtime.run(request = AssistantTurnRequest("Find something"), connection = script.connection).toList()
                assertEquals(AssistantRunEvent.Completed, events.last())
                assertEquals(3, script.requests.size)
            } }.awaitAll()
        }
    }

    @Test
    fun cancellationAndExceptionsReleaseClientAndOwnedEngineForEveryProtocol() = nativeTest {
        for (protocol in AssistantProtocol.entries) {
            for (cancel in listOf(true, false)) {
                val entered = CompletableDeferred<Unit>()
                val client = HttpClient(MockEngine) {
                    engine {
                        addHandler {
                            entered.complete(Unit)
                            if (cancel) awaitCancellation() else error("Synthetic network failure")
                        }
                    }
                }
                val engineJob = requireNotNull(client.engine.coroutineContext[Job])
                coroutineScope {
                    val job = launch {
                        PodnyaConnectionProbe(
                            connection = { AssistantConnection(baseUrl = "https://example.test/v1/", modelId = "test", apiKey = TEST_KEY, protocol = protocol) },
                            httpClient = { client },
                        ).verify()
                    }
                    withTimeout(5_000) { entered.await() }
                    if (cancel) job.cancelAndJoin() else job.join()
                    withTimeout(5_000) { engineJob.join() }
                    assertTrue(client.coroutineContext[Job]!!.isCompleted)
                    assertTrue(engineJob.isCompleted)
                }
            }
        }
    }

    @Test
    fun constructionFailureClosesTheOriginalEngine() = nativeTest {
        for (protocol in AssistantProtocol.entries) {
            var installations = 0
            val failClone = createClientPlugin("FailConfiguredClone") {
                check(++installations == 1) { "Synthetic configured-client construction failure" }
            }
            val client = HttpClient(MockEngine) {
                install(failClone)
                engine { addHandler { error("No request expected") } }
            }
            val engineJob = requireNotNull(client.engine.coroutineContext[Job])
            assertFailsWith<Exception> {
                PodnyaTransport(
                    connection = AssistantConnection(baseUrl = "https://example.test/v1/", modelId = "test", apiKey = TEST_KEY, protocol = protocol),
                    baseClient = client,
                )
            }
            withTimeout(5_000) { engineJob.join() }
            assertTrue(client.coroutineContext[Job]!!.isCompleted)
        }
    }

    @Test
    fun geminiManualModelCannotChangeTheRequestPathOrQuery() = nativeTest {
        val script = NativeScript(protocol = AssistantProtocol.GEMINI_GENERATE_CONTENT, replies = listOf(HttpReply(geminiText("OK"))))
        val connection = AssistantConnection(
            baseUrl = script.connection.baseUrl, modelId = "model/with?query#fragment", apiKey = TEST_KEY,
            protocol = AssistantProtocol.GEMINI_GENERATE_CONTENT,
        )
        PodnyaTransport(connection = connection, baseClient = script.client()).use { transport ->
            transport.executor.execute(prompt = prompt("manual") { user("hello") }, model = transport.model, tools = emptyList())
        }
        assertEquals(
            "/gateway/v1beta/" + assistantEndpointPath(baseUrl = connection.baseUrl, modelId = connection.modelId, protocol = connection.protocol),
            script.paths.single(),
        )
    }

    @Test
    fun claudeProbeRoundTripsThinkingAndRedactedBlocksBeforeStructureAndStream() = nativeTest {
        val exact = """[{"type":"thinking","thinking":"private","signature":"signed"},{"type":"redacted_thinking","data":"opaque"},{"type":"tool_use","id":"call-1","name":"podnya_echo","input":{"value":"podnya-capability"}}]"""
        val script = NativeScript(
            protocol = AssistantProtocol.ANTHROPIC_MESSAGES,
            replies = listOf(
                HttpReply(claude(content = exact, reason = "tool_use")),
                HttpReply(claudeText("Acknowledged")),
                HttpReply(claudeText("{\"value\":\"podnya-capability\"}")),
                HttpReply(body = claudeStream(), sse = true),
            ),
        )
        val result = PodnyaConnectionProbe(connection = { script.connection }, httpClient = script::client).verify()
        assertTrue(result.success, result.error.toString())
        assertEquals(4, script.requests.size)
        assertTrue(script.paths.all { it == "/gateway/v1/messages" })
        val messages = script.requests[1]["messages"] as JsonArray
        val assistant = messages.map { it.jsonObject }.first { it["role"].toString() == "\"assistant\"" }
        assertEquals(podnyaJson.parseToJsonElement(exact), assistant["content"])
        assertTrue(messages.last().toString().contains("call-1"))
    }

    @Test
    fun geminiProbePreservesNativeToolIdAndSignaturesWithoutQueryCredentials() = nativeTest {
        for (withId in listOf(true, false)) {
            val exactWithId = """[{"text":"private","thought":true,"thoughtSignature":"thinking-sig"},{"functionCall":{"id":"native-call","name":"podnya_echo","args":{"value":"podnya-capability"}},"thoughtSignature":"call-sig"},{"text":"Calling tool","thoughtSignature":"text-sig"}]"""
            val exact = if (withId) exactWithId else exactWithId.replace("\"id\":\"native-call\",", "")
            val script = NativeScript(
                protocol = AssistantProtocol.GEMINI_GENERATE_CONTENT,
                replies = listOf(
                    HttpReply(gemini(parts = exact)),
                    HttpReply(geminiText("Acknowledged")),
                    HttpReply(geminiText("{\"value\":\"podnya-capability\"}")),
                    HttpReply(body = geminiStream(), sse = true),
                ),
            )
            val result = PodnyaConnectionProbe(connection = { script.connection }, httpClient = script::client).verify()
            assertTrue(result.success, result.error.toString())
            assertEquals(4, script.requests.size)
            assertEquals("/gateway/v1beta/models/native-model:generateContent", script.paths.first())
            assertEquals("/gateway/v1beta/models/native-model:streamGenerateContent", script.paths.last())
            val contents = script.requests[1]["contents"] as JsonArray
            val model = contents.map { it.jsonObject }.first { it["role"].toString() == "\"model\"" }
            assertEquals(podnyaJson.parseToJsonElement(exact), model["parts"])
            if (withId) {
                assertTrue(contents.last().toString().contains("native-call"))
            } else {
                assertFalse(contents.last().toString().contains("\"id\""))
            }
            assertFalse(script.requests[1].toString().contains("context_engineering_is_the_way_to_go"))
        }
    }

    @Test
    fun nativeNonStreamingFailuresCannotBecomeSuccessfulToolOrStructuredResults() = nativeTest {
        val failures = listOf(
            AssistantProtocol.ANTHROPIC_MESSAGES to claude(content = "[]", reason = "max_tokens"),
            AssistantProtocol.ANTHROPIC_MESSAGES to claude(content = "[]", reason = "refusal"),
            AssistantProtocol.ANTHROPIC_MESSAGES to claude(content = "[]", reason = "pause_turn"),
            AssistantProtocol.GEMINI_GENERATE_CONTENT to gemini(parts = "[]", reason = "MAX_TOKENS"),
            AssistantProtocol.GEMINI_GENERATE_CONTENT to gemini(parts = "[]", reason = "SAFETY"),
            AssistantProtocol.GEMINI_GENERATE_CONTENT to """{"candidates":[],"promptFeedback":{"blockReason":"SAFETY"}}""",
        )
        for ((protocol, body) in failures) {
            val script = NativeScript(protocol = protocol, replies = listOf(HttpReply(body)))
            PodnyaTransport(connection = script.connection, baseClient = script.client()).use { transport ->
                val error = assertFailsWith<AssistantException> {
                    BudgetedExecutor(transport.executor).execute(
                        prompt = prompt("failure") { user("hello") }, model = transport.model, tools = emptyList(),
                    )
                }
                assertEquals(AssistantError.INVALID_OUTPUT, error.error)
            }
        }
    }

    @Test
    fun nativeStreamsRejectMissingTerminalTruncationRefusalAndErrorsAfterTerminal() = nativeTest {
        val claude = claudeStream()
        val gemini = geminiStream()
        val failures = listOf(
            AssistantProtocol.ANTHROPIC_MESSAGES to claude.substringBefore("data: {\"type\":\"message_stop\"}"),
            AssistantProtocol.ANTHROPIC_MESSAGES to claude.replace("end_turn", "max_tokens"),
            AssistantProtocol.ANTHROPIC_MESSAGES to claude.replace("end_turn", "refusal"),
            AssistantProtocol.ANTHROPIC_MESSAGES to (claude + event("""{"type":"error","error":{"type":"overloaded_error"}}""")),
            AssistantProtocol.GEMINI_GENERATE_CONTENT to gemini.replace(",\"finishReason\":\"STOP\"", ""),
            AssistantProtocol.GEMINI_GENERATE_CONTENT to gemini.replace("STOP", "MAX_TOKENS"),
            AssistantProtocol.GEMINI_GENERATE_CONTENT to gemini.replace("STOP", "SAFETY"),
            AssistantProtocol.GEMINI_GENERATE_CONTENT to (gemini + event("""{"error":{"code":500}}""")),
        )
        for ((protocol, body) in failures) {
            val script = NativeScript(protocol = protocol, replies = listOf(HttpReply(body = body, sse = true)))
            PodnyaTransport(connection = script.connection, baseClient = script.client()).use { transport ->
                val error = assertFailsWith<AssistantException> {
                    collectReply(BudgetedExecutor(transport.executor).executeStreaming(
                        prompt = prompt("failure") { user("hello") }, model = transport.model, tools = emptyList(),
                    )) {}
                }
                assertEquals(AssistantError.STREAM_INTERRUPTED, error.error)
            }
        }
    }

    @Test
    fun nativeStreamsRequireProtocolTerminationAndExcludeThoughts() = nativeTest {
        for ((protocol, body) in listOf(
            AssistantProtocol.ANTHROPIC_MESSAGES to claudeStream(),
            AssistantProtocol.ANTHROPIC_MESSAGES to claudeStream()
                .replace("\"index\":0", "\"index\":1")
                .replace(
                    event("""{"type":"content_block_start","index":1,"content_block":{"type":"text","text":""}}"""),
                    event("""{"type":"content_block_start","index":0,"content_block":{"type":"redacted_thinking","data":"opaque"}}""") +
                        event("""{"type":"content_block_stop","index":0}""") +
                        event("""{"type":"content_block_start","index":1,"content_block":{"type":"text","text":""}}"""),
                ).replace(
                    event("""{"type":"message_stop"}"""),
                    event("""{"type":"message_delta","delta":{},"usage":{"output_tokens":2}}""") +
                        event("""{"type":"message_stop"}"""),
                ),
            AssistantProtocol.GEMINI_GENERATE_CONTENT to geminiStream(),
        )) {
            val script = NativeScript(protocol = protocol, replies = listOf(HttpReply(body = body, sse = true)))
            val text = StringBuilder()
            PodnyaTransport(connection = script.connection, baseClient = script.client()).use { transport ->
                collectReply(transport.executor.executeStreaming(
                    prompt = prompt("native-stream") { user("hello") }, model = transport.model, tools = emptyList(),
                )) { text.append(it) }
            }
            assertEquals("OK", text.toString())
        }
    }
}

private class NativeScript(val protocol: AssistantProtocol, replies: List<HttpReply>) {
    private val pending = ArrayDeque(replies)
    val requests = mutableListOf<JsonObject>()
    val paths = mutableListOf<String>()
    val connection = AssistantConnection(
        baseUrl = if (protocol == AssistantProtocol.ANTHROPIC_MESSAGES) "https://example.test/gateway/v1/" else "https://example.test/gateway/v1beta/",
        modelId = "native-model", apiKey = TEST_KEY, protocol = protocol,
    )
    fun client() = HttpClient(SseMockEngine { request ->
        assertFalse(request.url.toString().contains(TEST_KEY))
        if (protocol == AssistantProtocol.ANTHROPIC_MESSAGES) {
            assertEquals(TEST_KEY, request.headers["x-api-key"])
            assertEquals("2023-06-01", request.headers["anthropic-version"])
        } else {
            assertEquals(TEST_KEY, request.headers["x-goog-api-key"])
        }
        requests += podnyaJson.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
        paths += request.url.encodedPath
        val reply = pending.removeFirst()
        respond(
            content = reply.body, status = reply.status,
            headers = headersOf(HttpHeaders.ContentType, if (reply.sse) "text/event-stream" else "application/json"),
        )
    }) { followRedirects = false }
}

private fun claude(content: String, reason: String = "end_turn") =
    """{"id":"message","type":"message","role":"assistant","model":"native-model","content":$content,"stop_reason":"$reason","usage":{"input_tokens":1,"output_tokens":1}}"""
private fun claudeText(text: String) = claude("""[{"type":"text","text":${podnyaJson.encodeToString(text)}}]""")
private fun gemini(parts: String, reason: String = "STOP") =
    """{"candidates":[{"content":{"role":"model","parts":$parts},"finishReason":"$reason","index":0}]}"""
private fun geminiText(text: String) = gemini("""[{"text":${podnyaJson.encodeToString(text)}}]""")
private fun event(raw: String) = "data: $raw\n\n"
private fun claudeStream() = listOf(
    """{"type":"message_start","message":{"id":"message","type":"message","role":"assistant","model":"native-model","content":[],"usage":{"input_tokens":1,"output_tokens":0}}}""",
    """{"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}""",
    """{"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"OK"}}""",
    """{"type":"content_block_stop","index":0}""",
    """{"type":"message_delta","delta":{"stop_reason":"end_turn"},"usage":{"output_tokens":1}}""",
    """{"type":"message_stop"}""",
).joinToString(separator = "", transform = ::event)
private fun geminiStream() = event("""{"candidates":[{"content":{"role":"model","parts":[{"text":"private","thought":true}]},"index":0}]}""") +
    event(geminiText("OK"))

private fun nativeTest(block: suspend () -> Unit) = runTest {
    withContext(Dispatchers.Default) { block() }
}
