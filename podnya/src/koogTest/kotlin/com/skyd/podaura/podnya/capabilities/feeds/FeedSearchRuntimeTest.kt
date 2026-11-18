package com.skyd.podaura.podnya.capabilities.feeds

import ai.koog.prompt.message.MessagePart
import com.skyd.podaura.api.model.bean.FeedQuery
import com.skyd.podaura.api.model.bean.FeedRecord
import com.skyd.podaura.api.model.repository.IFeedRepository
import com.skyd.podaura.podnya.capabilities.articles.RecordingCatalog
import com.skyd.podaura.podnya.capabilities.articles.model.repository.LocalArticleSearchRepository
import com.skyd.podaura.podnya.capabilities.feeds.model.bean.FeedSearchContent
import com.skyd.podaura.podnya.capabilities.feeds.model.bean.FeedSearchError
import com.skyd.podaura.podnya.capabilities.feeds.model.bean.FeedSearchQuery
import com.skyd.podaura.podnya.capabilities.feeds.model.repository.LocalFeedSearchRepository
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantHistoryTurn
import com.skyd.podaura.podnya.core.AssistantRunEvent
import com.skyd.podaura.podnya.core.AssistantTurnRequest
import com.skyd.podaura.podnya.model.bean.AssistantReplyContent
import com.skyd.podaura.podnya.runtime.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class FeedSearchRuntimeTest {
    @Test
    fun findsRechecksAndStreamsFeedsWithoutSendingTheirUrls() = runTest { withContext(Dispatchers.Default) {
        val catalog = Feeds()
        val articles = RecordingCatalog()
        val script = HttpScript(feedCall(), chatCompletion("done"), selection(), chatSse())
        val events = runtime(script, catalog, articles).run(AssistantTurnRequest("找一下我添加的音乐订阅"), com.skyd.podaura.podnya.runtime.testConnection()).toList()
        assertEquals(AssistantRunEvent.Completed, events.last())
        assertTrue(articles.queries.isEmpty())
        assertEquals(2, catalog.queries.size)
        assertEquals(listOf(URL), catalog.queries.last().withinFeedUrls)
        assertTrue(catalog.queries.last().keywords.isEmpty())
        val content = events.filterIsInstance<AssistantRunEvent.ContentReady>().single().content as FeedSearchContent
        assertEquals("Fresh music feed", content.feeds.single().feed.title)
        assertEquals(URL, content.feeds.single().feedUrl)
        assertEquals(400, content.feeds.single().feed.description.length)
        assertFalse(script.requests.toString().contains(URL))
        assertFalse(script.requests.toString().contains("private-token"))
        assertFalse(content.historySnapshot().payload.contains(URL))
        val tools = script.requests.first().getValue("tools").jsonArray
        assertEquals(setOf("search_articles", "search_feeds"), tools.map {
            it.jsonObject.getValue("function").jsonObject.getValue("name").jsonPrimitive.content
        }.toSet())

        val followup = HttpScript(feedCall(), chatCompletion("done"), selection(), chatSse())
        val next = runtime(followup, catalog).run(AssistantTurnRequest("再看看刚才的订阅", history = listOf(
            AssistantHistoryTurn("音乐订阅", "RESULTS", content.historySnapshot())
        )), com.skyd.podaura.podnya.runtime.testConnection()).toList()
        assertEquals(AssistantRunEvent.Completed, next.last())
        assertTrue(followup.requests.first().toString().contains("Fresh music feed"))
        assertFalse(followup.requests.toString().contains(URL))
    } }

    @Test
    fun bothCapabilitiesShareOneToolBudgetAndProduceSeparateCards() = runTest { withContext(Dispatchers.Default) {
        val feeds = Feeds()
        val articles = RecordingCatalog()
        val script = HttpScript(
            chatToolCalls("search_articles", "a" to "{}"),
            chatToolCalls("search_feeds", "f" to "{}", "f2" to "{}", "excess" to "{}"),
            chatCompletion("""{"kind":"RESULTS","text":"","outputs":{"articles":{"articles":[{"articleId":"a","reason":"Matches"}]},"feeds":{"feeds":[{"feedId":"feed-1","reason":"Matches"}]}}}"""),
            chatSse(),
        )
        val events = runtime(script, feeds, articles).run(AssistantTurnRequest("查找文章和订阅"), com.skyd.podaura.podnya.runtime.testConnection()).toList()
        assertEquals(AssistantRunEvent.Completed, events.last())
        assertEquals(2, articles.queries.size)
        assertEquals(3, feeds.queries.size) // Two admitted searches plus one final recheck.
        val content = events.filterIsInstance<AssistantRunEvent.ContentReady>().single().content as AssistantReplyContent
        assertEquals(2, content.parts.size)
        assertFalse(content.historySnapshot().payload.contains(URL))
    } }

    @Test
    fun forgedIdsGetOneRepairAndNeverBecomeCards() = runTest { withContext(Dispatchers.Default) {
        val script = HttpScript(feedCall(), chatCompletion("done"), selection("not-found"), selection("also-invalid"))
        val events = runtime(script, Feeds()).run(AssistantTurnRequest("订阅"), com.skyd.podaura.podnya.runtime.testConnection()).toList()
        assertEquals(AssistantRunEvent.Failed(AssistantError.INVALID_OUTPUT), events.last())
        assertEquals(4, script.requests.size)
        assertFalse(events.any { it is AssistantRunEvent.ContentReady })
    } }

    @Test
    fun emptySearchesAreNoMatchButDatabaseFailuresAndDeletedFeedsAreNot() = runTest { withContext(Dispatchers.Default) {
        val emptyScript = HttpScript(chatToolCalls("search_feeds", "1" to "{}", "2" to "{}", "3" to "{}"),
            chatCompletion("""{"kind":"NO_MATCH","text":"","outputs":{"feeds":{"feeds":[]}}}"""), chatSse())
        val events = runtime(emptyScript, Feeds(empty = true)).run(AssistantTurnRequest("订阅"), com.skyd.podaura.podnya.runtime.testConnection()).toList()
        assertEquals(AssistantRunEvent.Completed, events.last())
        val empty = events.filterIsInstance<AssistantRunEvent.ContentReady>().single().content as FeedSearchContent
        assertFalse(empty.hadCandidates)
        assertTrue(empty.feeds.isEmpty())

        val failed = runtime(HttpScript(feedCall()), Feeds(fail = true))
            .run(AssistantTurnRequest("订阅"), com.skyd.podaura.podnya.runtime.testConnection()).toList()
        assertEquals(AssistantRunEvent.Failed(FeedSearchError.LOCAL_SEARCH), failed.last())

        val deleted = runtime(HttpScript(feedCall(), chatCompletion("done"), selection()), Feeds(removeOnRecheck = true))
            .run(AssistantTurnRequest("订阅"), com.skyd.podaura.podnya.runtime.testConnection()).toList()
        assertEquals(AssistantRunEvent.Failed(FeedSearchError.RESULTS_CHANGED), deleted.last())
        assertFalse(deleted.any { it is AssistantRunEvent.ContentReady })
    } }

    @Test
    fun malformedArgumentsCountBeforeDecodeAndNeverReadTheDatabase() = runTest { withContext(Dispatchers.Default) {
        val feeds = Feeds()
        val script = HttpScript(chatToolCalls("search_feeds", "1" to "invalid", "2" to "{\"limit\":99}", "3" to "invalid", "4" to "{}"))
        val events = runtime(script, feeds).run(AssistantTurnRequest("订阅"), com.skyd.podaura.podnya.runtime.testConnection()).toList()
        assertEquals(AssistantRunEvent.Failed(AssistantError.BUDGET_EXHAUSTED), events.last())
        assertTrue(feeds.queries.isEmpty())
    } }

    @Test
    fun mutedFilterAndAppScopeCannotBeBroadened() = runTest {
        val feeds = Feeds()
        val tool = SearchFeedsTool(LocalFeedSearchRepository(feeds), listOf(URL))
        val query = FeedSearchQuery(keywords = listOf(" Music "), muted = true)
        assertNull(tool.admit(MessagePart.Tool.Call("1", "search_feeds", podnyaJson.encodeToString(query))))
        tool.execute(query)
        assertEquals(listOf(URL), feeds.queries.single().withinFeedUrls)
        assertEquals(listOf("Music"), feeds.queries.single().keywords)
        assertNotNull(tool.admit(MessagePart.Tool.Call("2", "search_feeds", "{}")))
    }

    @Test
    fun feedToolAlsoWorksThroughResponsesEndpoint() = runTest { withContext(Dispatchers.Default) {
        val script = HttpScript(
            responsesReply(responsesCall("f", name = "search_feeds")),
            responsesReply(responsesMessage("done")),
            responsesReply(responsesMessage("""{"kind":"RESULTS","text":"","outputs":{"feeds":{"feeds":[{"feedId":"feed-1","reason":"Matches"}]}}}""")),
            responsesStream(),
        )
        val runtime = KoogAssistantRuntime(testCapabilities(LocalArticleSearchRepository(RecordingCatalog()),
            LocalFeedSearchRepository(Feeds())), script::client)
        val events = runtime.run(AssistantTurnRequest("音乐订阅"), astra()).toList()
        assertEquals(AssistantRunEvent.Completed, events.last())
        assertTrue(script.paths.all { it == "/v1/responses" })
        assertFalse(script.requests.toString().contains(URL))
    } }

    private fun runtime(script: HttpScript, feeds: Feeds, articles: RecordingCatalog = RecordingCatalog()) =
        KoogAssistantRuntime(testCapabilities(LocalArticleSearchRepository(articles), LocalFeedSearchRepository(feeds)),
            script::client)

    private fun feedCall() = chatToolCalls("search_feeds", "f" to "{\"keywords\":[\"music\"]}")
    private fun selection(id: String = "feed-1") = chatCompletion(
        """{"kind":"RESULTS","text":"","outputs":{"feeds":{"feeds":[{"feedId":"$id","reason":"Music discussions"}]}}}"""
    )

    private class Feeds(val empty: Boolean = false, val fail: Boolean = false, val removeOnRecheck: Boolean = false) : IFeedRepository {
        val queries = mutableListOf<FeedQuery>()
        override suspend fun query(query: FeedQuery): List<FeedRecord> {
            queries += query
            if (fail) error("private database failure")
            if (empty || removeOnRecheck && query.withinFeedUrls != null) return emptyList()
            return listOf(FeedRecord(URL, if (query.withinFeedUrls == null) "Music feed" else "Fresh music feed",
                null, "<p>" + "Music description. ".repeat(100) + "</p>", null, query.muted ?: false))
        }
    }

    companion object { private const val URL = "https://example.test/feed?token=private-token" }
}
