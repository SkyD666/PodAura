package com.skyd.podaura.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.window.Dialog
import androidx.navigation3.runtime.NavKey
import com.skyd.podaura.api.model.repository.IArticleRepository
import com.skyd.podaura.api.model.repository.IAssistantDownloadRepository
import com.skyd.podaura.api.model.repository.IFeedRepository
import com.skyd.podaura.di.initKoin
import com.skyd.podaura.podnya.capabilities.articles.model.bean.ArticleSearchContent
import com.skyd.podaura.podnya.capabilities.feeds.model.bean.FeedSearchContent
import com.skyd.podaura.podnya.model.bean.AssistantReplyContent
import com.skyd.podaura.podnya.core.AssistantConnection
import com.skyd.podaura.podnya.core.AssistantHistoryCodec
import com.skyd.podaura.podnya.core.AssistantRunEvent
import com.skyd.podaura.podnya.core.AssistantRuntime
import com.skyd.podaura.podnya.core.AssistantTurnRequest
import com.skyd.podaura.podnya.core.CapabilityRecord
import com.skyd.podaura.podnya.core.UnreadableContent
import com.skyd.podaura.podnya.model.db.ConversationRecordCodec
import com.skyd.podaura.podnya.ui.AssistantCapabilityUi
import com.skyd.podaura.podnya.ui.podnyaContent
import com.skyd.podaura.ui.screen.article.ArticleRoute
import com.skyd.podaura.ui.screen.read.ReadRoute
import com.skyd.podnya.resources.Res
import com.skyd.podnya.resources.podnya_download_open
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.getString
import org.koin.core.context.stopKoin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

@OptIn(ExperimentalTestApi::class)
class PodnyaPluginTest {
    private val article = record("articles", """{"kind":"RESULTS","articles":[{"article":{"articleId":"local-id","title":"Saved episode","sourceName":"","snippet":""},"reason":"Earlier result"}]}""")
    private val feed = record("feeds", """{"feeds":[{"feed":{"feedId":"F1","title":"Saved feed","description":"","muted":false},"reason":"Earlier result","feedUrl":"https://feed.test/"}],"hadCandidates":true,"resultsChanged":false}""")
    private val download = record(
        "downloads",
        """
        {"choices":[{
            "id":"format", "title":"Episode format",
            "options":[{"id":"1", "title":"MP3", "targets":[{
                "type":"com.skyd.podaura.api.model.bean.AssistantDownloadTarget.Attachment",
                "articleId":"local-id", "url":"https://media.test/episode.mp3"
            }]},{"id":"2", "title":"Bonus MP3", "targets":[{
                "type":"com.skyd.podaura.api.model.bean.AssistantDownloadTarget.DirectLink",
                "url":"https://media.test/bonus.mp3"
            }]}]
        }]}
        """.trimIndent(),
    )

    @Test
    fun removingAndRestoringOneRegistrationChangesCardsActionsAndHistoryTogether() {
        var saved = download
        val testDownloadPlugin = createDownloadPlugin { request, dismiss ->
            Dialog(onDismissRequest = dismiss) {
                BasicText(text = request.batchId, modifier = Modifier.clickable(onClick = dismiss))
            }
        }
        val installed = builtInPodnyaPlugins.map { if (it === downloadPlugin) testDownloadPlugin else it }
        for (plugins in listOf(installed, installed - testDownloadPlugin, installed.reversed())) {
            val app = initKoin { modules(podnyaAppModule(plugins)) }
            try {
                app.koin.declare<IArticleRepository>(TestArticles)
                app.koin.declare<IFeedRepository>(TestFeeds)
                val enabled = testDownloadPlugin in plugins
                if (enabled) {
                    app.koin.declare<IAssistantDownloadRepository>(TestDownloads())
                } else {
                    assertNull(app.koin.getOrNull<IAssistantDownloadRepository>())
                }
                val ids = app.koin.getAll<AssistantHistoryCodec>().map { it.capabilityId }.toSet()
                assertEquals(if (enabled) setOf("articles", "feeds", "downloads") else setOf("articles", "feeds"), ids)
                val codec = app.koin.get<ConversationRecordCodec>()
                val contents = listOf(article, feed, saved).map { checkNotNull(codec.content(it)) }
                if (!enabled) {
                    saved = checkNotNull(codec.content(assertIs<UnreadableContent>(contents.last())))
                    assertEquals(download, saved)
                }
                val uis = app.koin.getAll<AssistantCapabilityUi>()
                val scope = app.koin.createScope<PodnyaPluginTest>("plugin-test")
                val open = runBlocking { getString(Res.string.podnya_download_open) }
                runDesktopComposeUiTest(width = 700, height = 850) {
                    val routes = mutableListOf<NavKey>()
                    val conversationId = Uuid.random()
                    setContent {
                        val onAction = plugins.bindNavigation(scope) { routes.add(it) }
                        LazyColumn {
                            contents.forEachIndexed { index, content ->
                                podnyaContent(
                                    turnId = index.toLong(),
                                    content = content,
                                    showFallbackText = true,
                                    formatDate = { "" },
                                    onAction = { onAction(conversationId, it) },
                                    uis = uis,
                                )
                            }
                        }
                    }
                    onNodeWithText("Saved episode").performClick()
                    waitUntil { routes.contains(ReadRoute("local-id")) }
                    onNodeWithText("Saved feed").performClick()
                    waitUntil { routes.contains(ArticleRoute(feedUrls = listOf("https://feed.test/"))) }
                    if (enabled) {
                        onNodeWithText("1. MP3").assertExists()
                        onNodeWithText("2. Bonus MP3").assertExists()
                        onNodeWithText(open).performClick()
                        val sheet = onNodeWithText("$conversationId:2")
                        sheet.performClick()
                        sheet.assertDoesNotExist()
                        assertEquals(2, routes.size)
                    } else {
                        onNodeWithText(open).assertDoesNotExist()
                        onNodeWithText("Saved downloads").assertExists()
                    }
                }
            } finally {
                stopKoin()
            }
        }
    }

    @Test
    fun runtimeOffersOnlyInstalledFeaturesAndChecksDirectLinksWithoutSearch() = runBlocking {
        val link = "https://media.test/file.mp3"
        for (plugins in listOf(builtInPodnyaPlugins, builtInPodnyaPlugins - downloadPlugin, listOf(downloadPlugin))) {
            val app = initKoin { modules(podnyaAppModule(plugins)) }
            val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
            try {
                val enabled = downloadPlugin in plugins
                val downloads = TestDownloads()
                if (enabled) app.koin.declare<IAssistantDownloadRepository>(downloads)
                if (articleSearchPlugin in plugins) app.koin.declare<IArticleRepository>(TestArticles)
                app.koin.declare<IFeedRepository>(TestFeeds)
                val question = if (enabled) "Download $link" else "Find local articles and subscriptions. Download $link"
                val requests = mutableListOf<String>()
                val plan = """{"kind":"RESULTS","text":"","outputs":{"downloads":{"directLinks":[{"url":"$link","before":"Download "}]}}}"""
                val found = """{"kind":"RESULTS","text":"","outputs":{"articles":{"articles":[{"articleId":"local-id","reason":"Found"}]},"feeds":{"feeds":[{"feedId":"feed-1","reason":"Found"}]}}}"""
                val replies = if (enabled) {
                    listOf(textReply("Ready"), textReply(plan))
                } else {
                    val calls = listOf("search_articles", "search_feeds").mapIndexed { index, name ->
                        """{"id":"call-$index","type":"function","function":{"name":"$name","arguments":"{}"}}"""
                    }
                    listOf(
                        completion(message = """{"role":"assistant","tool_calls":[${calls.joinToString(",")}]}""", finishReason = "tool_calls"),
                        textReply("Found both"),
                        textReply(found),
                    )
                }
                server.createContext("/v1/chat/completions") { exchange ->
                    val request = exchange.requestBody.bufferedReader().use { it.readText() }
                    val index = requests.size
                    requests += request
                    val streaming = Json.parseToJsonElement(request).jsonObject["stream"]?.jsonPrimitive?.content == "true"
                    val response = if (streaming) {
                        val chunk = """{"id":"local","object":"chat.completion.chunk","created":0,"model":"test","choices":[{"index":0,"delta":{"role":"assistant","content":"Both found"},"finish_reason":"stop"}]}"""
                        "data: $chunk\n\ndata: [DONE]\n\n"
                    } else {
                        replies.getOrElse(index) { textReply("Unexpected extra request") }
                    }
                    val body = response.toByteArray()
                    exchange.responseHeaders.add("Content-Type", if (streaming) "text/event-stream" else "application/json")
                    exchange.sendResponseHeaders(200, body.size.toLong())
                    exchange.responseBody.use { it.write(body) }
                }
                server.start()
                val events = app.koin.get<AssistantRuntime>().run(
                    request = AssistantTurnRequest(question),
                    connection = AssistantConnection(
                        baseUrl = "http://127.0.0.1:${server.address.port}/v1/",
                        modelId = "test",
                        apiKey = "synthetic",
                    ),
                ).toList()
                assertEquals(AssistantRunEvent.Completed, events.last())
                assertEquals(enabled, events.any { it is AssistantRunEvent.Handoff })
                assertEquals(link.takeIf { enabled }, downloads.checkedLink)
                assertEquals(enabled, "Capability downloads:" in requests.first())
                val tools = Json.parseToJsonElement(requests.first()).jsonObject["tools"]?.jsonArray.orEmpty().map {
                    it.jsonObject.getValue("function").jsonObject.getValue("name").jsonPrimitive.content
                }.toSet()
                assertEquals(articleSearchPlugin in plugins, "search_articles" in tools)
                assertEquals(feedSearchPlugin in plugins, "search_feeds" in tools)
                if (!enabled) {
                    val toolResults = Json.parseToJsonElement(requests[1]).jsonObject.getValue("messages").jsonArray
                        .map { it.jsonObject }.filter { it["role"]?.jsonPrimitive?.content == "tool" }
                    assertTrue(toolResults.any { "Local title" in it.toString() })
                    assertTrue(toolResults.any { "Saved feed" in it.toString() })
                    val reply = assertIs<AssistantReplyContent>(events.filterIsInstance<AssistantRunEvent.ContentReady>().single().content)
                    val articleCard = reply.parts.filterIsInstance<ArticleSearchContent>().single()
                    val feedCard = reply.parts.filterIsInstance<FeedSearchContent>().single()
                    assertEquals("Local title", articleCard.answer?.articles?.single()?.article?.title)
                    assertEquals("https://feed.test/", feedCard.feeds.single().feedUrl)
                }
            } finally {
                server.stop(0)
                stopKoin()
            }
        }
    }

    private fun textReply(text: String) =
        completion(message = """{"role":"assistant","content":${JsonPrimitive(text)}}""", finishReason = "stop")

    private fun completion(message: String, finishReason: String) =
        """{"id":"local","object":"chat.completion","created":0,"model":"test","choices":[{"index":0,"finish_reason":"$finishReason","message":$message}]}"""

    private fun record(id: String, payload: String) = Json.encodeToString(
        CapabilityRecord(capabilityId = id, version = 1, payload = payload, fallback = "Saved $id")
    )
}
