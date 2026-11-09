package com.skyd.podaura.podnya.capabilities.articles

import com.skyd.podaura.api.model.bean.ArticleSearchScope
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchContent
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchContext
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchPhase
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchProgress
import com.skyd.podaura.podnya.capabilities.articles.model.bean.AssistantArticle
import com.skyd.podaura.podnya.capabilities.articles.model.bean.RecommendedArticle
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ValidatedAssistantAnswer
import com.skyd.podaura.podnya.capabilities.articles.model.repository.LocalArticleSearchRepository
import com.skyd.podaura.podnya.runtime.emptyFeedSource
import com.skyd.podaura.podnya.core.AssistantAnswerKind
import com.skyd.podaura.podnya.core.AssistantContext
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantHistorySnapshot
import com.skyd.podaura.podnya.core.AssistantHistoryTurn
import com.skyd.podaura.podnya.core.AssistantPhase
import com.skyd.podaura.podnya.core.AssistantRunEvent
import com.skyd.podaura.podnya.core.AssistantTurnRequest
import com.skyd.podaura.podnya.runtime.HttpReply
import com.skyd.podaura.podnya.runtime.HttpScript
import com.skyd.podaura.podnya.runtime.testCapabilities
import com.skyd.podaura.podnya.runtime.KoogAssistantRuntime
import com.skyd.podaura.podnya.runtime.TEST_KEY
import com.skyd.podaura.podnya.runtime.chatCompletion as completion
import com.skyd.podaura.podnya.runtime.chatSse as sse
import com.skyd.podaura.podnya.runtime.chatToolCalls
import com.skyd.podaura.podnya.runtime.podnyaJson
import com.skyd.podaura.podnya.runtime.testConnection
import com.skyd.podaura.podnya.settings.AssistantConnection
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.http.HttpStatusCode
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

class KoogArticleSearchRuntimeTest {
    @Test
    fun realChatCompletionsToolSelectionAndSsePaths() = runTest { withContext(Dispatchers.Default) {
        val catalog = RecordingCatalog()
        val script = HttpScript(toolCalls("s1" to "{}"), completion("Finished searching."),
            selection("a"), sse())
        val events = script.assistant(catalog).run(AssistantTurnRequest("Find local articles")).toList()
        assertEquals(2, catalog.queries.size) // One search and one final recheck.
        assertEquals(ArticleSearchScope.All, catalog.scope)
        assertEquals(listOf("a"), catalog.queries.last().withinArticleIds)
        assertTrue(catalog.queries.last().keywords.isEmpty())
        assertEquals(4, script.requests.size)
        assertTrue(script.paths.all { it == "/gateway/v1/chat/completions" })
        assertTrue(script.requests.all { it["response_format"] == null }) // Manual schema; no unverified native capability.
        assertTrue(script.requests.first().containsKey("tools"))
        assertTrue(script.requests.takeLast(2).all { it["tools"] == null || it["tools"] == JsonArray(emptyList()) })
        val toolResult = script.requests[1].getValue("messages").jsonArray.map { it.jsonObject }
            .single { it["role"]?.jsonPrimitive?.content == "tool" }
        assertEquals("s1", toolResult["tool_call_id"]!!.jsonPrimitive.content)
        assertTrue(toolResult.toString().contains("Local snippet"))
        val candidate = podnyaJson.parseToJsonElement(toolResult.getValue("content").jsonPrimitive.content)
            .jsonObject.getValue("page").jsonObject.getValue("items").jsonArray.single().jsonObject
        assertEquals(JsonPrimitive(0L), candidate["dateMillis"])
        assertFalse(candidate.containsKey("publishedAt"))
        val ready = events.filterIsInstance<AssistantRunEvent.ContentReady>().single()
        val recommended = (ready.content as ArticleSearchContent).answer!!.articles.single().article
        assertEquals("Fresh local title", recommended.title)
        assertEquals(1_700_000_000_000L, recommended.dateMillis)
        assertEquals("Found one.", events.filterIsInstance<AssistantRunEvent.ReplyDelta>().joinToString("") { it.text })
        assertTrue(events.indexOf(ready) < events.indexOfFirst { it is AssistantRunEvent.ReplyDelta })
        assertEquals(AssistantRunEvent.Completed, events.last())
        assertFalse(script.requests.any { it.toString().contains(TEST_KEY) })
    } }

    @Test
    fun genericContextAndHistoryPreserveArticleScopeAndKnownIdsWithoutDecodingOtherCapabilities() = runTest {
        withContext(Dispatchers.Default) {
            val historyContent = ArticleSearchContent.recommendations(ValidatedAssistantAnswer(
                AssistantAnswerKind.RESULTS, "", listOf(RecommendedArticle(
                    AssistantArticle("a", "Previous article title", "Previous catalog", snippet = "Prior snippet"),
                    "Previous reason",
                )),
            )).historySnapshot()!!
            val foreign = AssistantHistorySnapshot("other-capability", "Other summary", "foreign private payload")
            val scope = ArticleSearchScope.Restricted(feedUrls = setOf("authorized"))
            val script = HttpScript(toolCalls("1" to "{\"withinArticleIds\":[\"a\"]}"), completion("done"),
                selection("a"), sse())
            val catalog = RecordingCatalog()
            val events = script.assistant(catalog).run(AssistantTurnRequest(
                "Search these articles again", context = ArticleSearchContext(scope), history = listOf(
                    AssistantHistoryTurn("Earlier question", "Earlier reply", historyContent),
                    AssistantHistoryTurn("Other question", "Other reply", foreign),
                ),
            )).toList()
            assertEquals(AssistantRunEvent.Completed, events.last())
            assertEquals(scope, catalog.scope)
            assertEquals(listOf("a"), catalog.queries.first().withinArticleIds)
            val initial = script.requests.first().getValue("messages").jsonArray.map { it.jsonObject }
            val history = initial.first { it["role"] == JsonPrimitive("assistant") }
                .getValue("content").jsonPrimitive.content.let { podnyaJson.parseToJsonElement(it).jsonObject }
            assertEquals(setOf("reply", "outputs"), history.keys)
            assertEquals("Earlier reply", history.getValue("reply").jsonPrimitive.content)
            assertTrue(history.toString().contains("Previous article title"))
            assertFalse(script.requests.any { it.toString().contains(foreign.payload) })
            val progress = events.filterIsInstance<AssistantRunEvent.Progress>().map { it.progress }
            assertEquals(AssistantPhase.PREPARING, progress.first())
            assertTrue(progress.contains(AssistantPhase.PROCESSING))
            assertEquals(AssistantPhase.STREAMING, progress.last())
            val ready = events.filterIsInstance<AssistantRunEvent.ContentReady>().single().content as ArticleSearchContent
            assertEquals("Fresh local title", ready.answer!!.articles.single().article.title)
        }
    }

    @Test
    fun unknownContextFailsBeforeNetworkOrArticleAccess() = runTest {
        val context = object : AssistantContext {
            override fun snapshot(): AssistantContext = this
        }
        val catalog = RecordingCatalog()
        val script = HttpScript()
        val events = script.assistant(catalog).run(AssistantTurnRequest("Find articles", context)).toList()
        assertEquals(AssistantRunEvent.Failed(AssistantError.INVALID_OUTPUT), events.last())
        assertTrue(script.requests.isEmpty())
        assertTrue(catalog.queries.isEmpty())
    }

    @Test
    fun selectionUsesCompletedSearchesAndCannotPublishItsDraftAsVerifiedAppState() = runTest {
        withContext(Dispatchers.Default) {
            val misleading = "当前搜索已关闭，暂时无法为你提供经过核验的罗大佑相关文章。候选内容中有与罗大佑相关的内容，但尚未核验，因此暂不能列出推荐。这不代表没有匹配文章。"
            val query = "{\"keywords\":[\"罗大佑\"]}"
            val script = HttpScript(toolCalls("1" to query, "2" to query, "3" to query),
                selection("a", text = misleading), sse(finishReason = null))
            val catalog = RecordingCatalog()
            val events = script.assistant(catalog).run(AssistantTurnRequest("找罗大佑的相关文章")).toList()
            val selectionMessages = script.requests[1].getValue("messages").jsonArray.map { it.jsonObject }
            val system = selectionMessages.filter { it["role"] == JsonPrimitive("system") }.joinToString()
            assertTrue(system.contains("Select and rank local articles already retrieved"))
            assertFalse(system.contains("Use search_articles"))
            assertFalse(selectionMessages.toString().contains("Search is closed"))
            assertTrue(script.requests[1]["tools"] == null || script.requests[1]["tools"] == JsonArray(emptyList()))
            val snapshotText = selectionMessages.mapNotNull { (it["content"] as? JsonPrimitive)?.content }
                .single { it.startsWith("App capability snapshots:") }.substringAfter('\n')
            val snapshot = podnyaJson.parseToJsonElement(snapshotText).jsonObject.getValue("articles").jsonObject
            assertEquals(JsonPrimitive(3), snapshot["successfulSearches"])
            assertEquals("a", snapshot.getValue("candidates").jsonArray.single().jsonObject
                .getValue("hit").jsonObject.getValue("articleId").jsonPrimitive.content)
            val reply = script.requests.last().getValue("messages").jsonArray.last().jsonObject
                .getValue("content").jsonPrimitive.content.let { podnyaJson.parseToJsonElement(it).jsonObject }
            assertEquals(JsonPrimitive(3), reply.getValue("outputs").jsonObject.getValue("articles").jsonObject["successfulSearches"])
            assertEquals(JsonPrimitive(true), reply.getValue("outputs").jsonObject.getValue("articles").jsonObject["hadCandidates"])
            assertFalse(script.requests.last().toString().contains(misleading))
            val answer = (events.filterIsInstance<AssistantRunEvent.ContentReady>().single().content as ArticleSearchContent).answer!!
            assertEquals("", answer.text) // Also prevents the interrupted-stream UI fallback from repeating it.
            assertEquals("Fresh local title", answer.articles.single().article.title)
            assertEquals(listOf("a"), catalog.queries.last().withinArticleIds)
            assertTrue(catalog.queries.last().keywords.isEmpty())
            assertEquals(AssistantRunEvent.Failed(AssistantError.STREAM_INTERRUPTED), events.last())
            assertFalse(events.toString().contains(misleading))
        }
    }

    @Test
    fun reasoningIsRetainedInToolExchangeButExcludedFromFinalPromptsAndUi() = runTest { withContext(Dispatchers.Default) {
        val reasoning = "Internal reasoning required by the compatible service"
        val script = HttpScript(toolCalls("s1" to "{}", reasoning = reasoning), completion("Finished searching."),
            selection("a"), sse())
        val events = script.assistant(RecordingCatalog()).run(AssistantTurnRequest("Find local articles")).toList()
        val assistantMessage = script.requests[1].getValue("messages").jsonArray.map { it.jsonObject }
            .single { it["role"] == JsonPrimitive("assistant") }
        assertEquals(reasoning, assistantMessage.getValue("reasoning_content").jsonPrimitive.content)
        assertEquals("s1", assistantMessage.getValue("tool_calls").jsonArray.single().jsonObject
            .getValue("id").jsonPrimitive.content)
        assertTrue(script.requests.takeLast(2).none { it.toString().contains(reasoning) })
        assertFalse(events.toString().contains(reasoning))
        assertEquals("Found one.", events.filterIsInstance<AssistantRunEvent.ReplyDelta>().joinToString("") { it.text })
        assertEquals(AssistantRunEvent.Completed, events.last())
    } }

    @Test
    fun multipleCallsInOneResponseAreSequentialAndCannotBypassBudget() = runTest { withContext(Dispatchers.Default) {
        val catalog = RecordingCatalog()
        val script = HttpScript(
            toolCalls("bad" to "{", "valid" to "{}", "unknown" to "{\"withinArticleIds\":[\"invented\"]}", "excess" to "{}"),
            selection("a"), sse(),
        )
        val events = script.assistant(catalog).run(AssistantTurnRequest("Find articles")).toList()
        assertEquals(2, catalog.queries.size) // One search and one final recheck.
        assertEquals(3, script.requests.size)
        assertEquals(AssistantRunEvent.Completed, events.last())
        // Finalization rebuilds a complete snapshot, with no orphan tool calls or results.
        val finalMessages = script.requests[1].getValue("messages").jsonArray
        assertFalse(finalMessages.any { "tool_calls" in it.jsonObject || it.jsonObject["role"] == JsonPrimitive("tool") })
        assertTrue(finalMessages.toString().contains("successfulSearches"))
        assertTrue(finalMessages.toString().contains("queryIndex"))
    } }

    @Test
    fun exactlyThreeCallsInOneResponseReachSourceAndThenFinalize() = runTest { withContext(Dispatchers.Default) {
        val catalog = RecordingCatalog()
        val script = HttpScript(toolCalls("a" to "{}", "b" to "{}", "c" to "{}"), selection("a"), sse())
        assertEquals(AssistantRunEvent.Completed,
            script.assistant(catalog).run(AssistantTurnRequest("Find articles")).toList().last())
        assertEquals(4, catalog.queries.size) // Three searches and one final recheck.
        assertEquals(3, script.requests.size)
    } }

    @Test
    fun unknownSelectionGetsOnlyOneRepair() = runTest { withContext(Dispatchers.Default) {
        val repaired = HttpScript(toolCalls("1" to "{}"), completion("done"), selection("invented"), selection("a"), sse())
        val good = repaired.assistant(RecordingCatalog()).run(AssistantTurnRequest("Find articles")).toList()
        assertEquals(AssistantRunEvent.Completed, good.last())
        assertEquals(5, repaired.requests.size)
        assertTrue(repaired.requests[3].toString().contains("Previous selection was invalid"))
        val invalid = HttpScript(toolCalls("1" to "{}"), completion("done"), selection("invented"), selection("invented"))
        val failed = invalid.assistant(RecordingCatalog()).run(AssistantTurnRequest("Find articles")).toList()
        assertEquals(AssistantError.INVALID_OUTPUT, (failed.last() as AssistantRunEvent.Failed).error)
        assertFalse(failed.any { it is AssistantRunEvent.ContentReady })
        assertEquals(4, invalid.requests.size)
    } }

    @Test
    fun interruptedSsePreservesValidatedCardsAndNeverCompletes() = runTest { withContext(Dispatchers.Default) {
        val script = HttpScript(toolCalls("1" to "{}"), completion("done"), selection("a"), sse(finishReason = null))
        val events = script.assistant(RecordingCatalog()).run(AssistantTurnRequest("Find articles")).toList()
        assertTrue(events.any { it is AssistantRunEvent.ContentReady })
        assertTrue(events.any { it is AssistantRunEvent.ReplyDelta })
        assertFalse(events.contains(AssistantRunEvent.Completed))
        assertEquals(AssistantError.STREAM_INTERRUPTED, (events.last() as AssistantRunEvent.Failed).error)
    } }

    @Test
    fun authenticationErrorIsSanitizedAndNeverRetried() = runTest { withContext(Dispatchers.Default) {
        val script = HttpScript(HttpReply("secret server response $TEST_KEY", status = HttpStatusCode.Unauthorized))
        val events = script.assistant(RecordingCatalog()).run(AssistantTurnRequest("Find articles")).toList()
        assertEquals(AssistantRunEvent.Failed(AssistantError.AUTHENTICATION), events.last())
        assertEquals(1, script.requests.size)
        assertFalse(events.toString().contains(TEST_KEY))
    } }

    @Test
    fun quotaRateAndUnknown429ErrorsStayDistinctForChatAndVerificationWithoutRetries() = runTest {
        withContext(Dispatchers.Default) {
            fun body(code: String? = null, type: String? = null) = buildJsonObject {
                put("error", buildJsonObject {
                    code?.let { put("code", it) }
                    type?.let { put("type", it) }
                    put("message", "Private server message with insufficient_quota and $TEST_KEY")
                })
            }.toString()
            val cases = buildList {
                listOf("insufficient_quota", "credit_balance_exhausted", "organization_spend_limit_exceeded",
                    "project_spend_limit_exceeded", "organization_usage_limit_exceeded").forEach {
                    add(body(code = it, type = "rate_limit_error") to AssistantError.QUOTA_EXCEEDED)
                }
                add(body(type = "insufficient_quota") to AssistantError.QUOTA_EXCEEDED)
                listOf("rate_limit_exceeded", "rate_limit_error", "slow_down").forEach {
                    add(body(code = it) to AssistantError.RATE_LIMIT)
                }
                add(body(type = "rate_limit_error") to AssistantError.RATE_LIMIT)
                // Messages alone must not invent a quota diagnosis; malformed bodies stay safe.
                listOf(body(), body(code = "unknown_provider_code"), "[]", "{\"error\":[]}",
                    "{\"error\":{\"code\":{},\"type\":42}}", "invalid JSON $TEST_KEY",
                    body(code = "insufficient_quota") + " ".repeat(16_384)).forEach {
                    add(it to AssistantError.REQUEST_LIMITED)
                }
            }
            for ((body, expected) in cases) {
                val reply = HttpReply(body, status = HttpStatusCode.TooManyRequests)
                val script = HttpScript(reply, reply)
                val catalog = RecordingCatalog()
                val assistant = script.assistant(catalog)
                val events = assistant.run(AssistantTurnRequest("Find articles")).toList()
                assertEquals(AssistantRunEvent.Failed(expected), events.last())
                assertEquals(1, script.requests.size)
                val verification = assistant.verify()
                assertFalse(verification.success)
                assertEquals(expected, verification.error)
                assertEquals(2, script.requests.size)
                assertTrue(catalog.queries.isEmpty())
                assertFalse(events.toString().contains(TEST_KEY))
                assertFalse(verification.toString().contains(TEST_KEY))
            }
        }
    }

    @Test
    fun missingOrDuplicateSseTerminatorIsNotSuccess() = runTest { withContext(Dispatchers.Default) {
        val normal = sse()
        listOf(normal.copy(body = normal.body.removeSuffix("data: [DONE]\n\n")),
            normal.copy(body = normal.body + "data: [DONE]\n\n")).forEach { broken ->
            val script = HttpScript(toolCalls("1" to "{}"), completion("done"), selection("a"), broken)
            val events = script.assistant(RecordingCatalog()).run(AssistantTurnRequest("Find articles")).toList()
            assertTrue(events.any { it is AssistantRunEvent.ContentReady })
            assertEquals(AssistantError.STREAM_INTERRUPTED, (events.last() as AssistantRunEvent.Failed).error)
        }
    } }

    @Test
    fun cancellationStopsTheRequestAndDoesNotStartSelection() = runTest { withContext(Dispatchers.Default) {
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        var requests = 0
        val client = HttpClient(MockEngine {
            requests++
            started.complete(Unit)
            try { awaitCancellation() } finally { cancelled.complete(Unit) }
        }) { followRedirects = false }
        val assistant = KoogAssistantRuntime(testCapabilities(LocalArticleSearchRepository(RecordingCatalog()), emptyFeedSource()), { testConnection() }, { client })
        val events = mutableListOf<AssistantRunEvent>()
        val job = launch { assistant.run(AssistantTurnRequest("Find articles")).toList(events) }
        withTimeout(5_000) { started.await() }
        job.cancelAndJoin()
        withTimeout(5_000) { cancelled.await() }
        assertEquals(1, requests)
        assertFalse(events.any { it is AssistantRunEvent.Failed || it is AssistantRunEvent.Completed })
    } }
}

internal fun HttpScript.assistant(catalog: RecordingCatalog, connection: AssistantConnection = testConnection()) =
    KoogAssistantRuntime(testCapabilities(LocalArticleSearchRepository(catalog), emptyFeedSource()), { connection }, ::client)

private fun toolCalls(vararg calls: Pair<String, String>, reasoning: String? = null) =
    chatToolCalls("search_articles", *calls, reasoning = reasoning)

private fun selection(id: String, text: String = "Found a matching local article.") = completion(buildJsonObject {
    put("kind", "RESULTS"); put("text", text)
    put("outputs", buildJsonObject { put("articles", buildJsonObject {
        put("articles", buildJsonArray { add(buildJsonObject { put("articleId", id); put("reason", "Matches the request.") }) })
    }) })
}.toString())

