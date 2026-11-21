package com.skyd.podaura.podnya.runtime

import com.skyd.podaura.podnya.capabilities.articles.RecordingCatalog
import com.skyd.podaura.podnya.capabilities.articles.model.repository.LocalArticleSearchRepository
import com.skyd.podaura.podnya.core.AssistantConnection
import com.skyd.podaura.podnya.core.AssistantRunEvent
import com.skyd.podaura.podnya.core.AssistantTurnRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FixedConnectionsTest {
    @Test
    fun parallelRunsUseExplicitConnectionsWithoutReadingTheVerificationConfiguration() = runTest {
        withContext(Dispatchers.Default) {
            val scripts = List(2) {
                HttpScript(chatCompletion("""{"kind":"CLARIFICATION","text":"What topic?","outputs":{}}"""))
            }
            val connections = listOf(
                AssistantConnection("https://first.example/one/", "first-model", TEST_KEY),
                AssistantConnection("https://second.example/two/", "second-model", TEST_KEY),
            )
            scripts.mapIndexed { index, script -> async {
                val runtime = KoogAssistantRuntime(
                    testCapabilities(LocalArticleSearchRepository(RecordingCatalog()), emptyFeedSource()),
                    script::client,
                )
                assertEquals(AssistantRunEvent.Completed,
                    runtime.run(AssistantTurnRequest("Find something"), connections[index]).toList().last())
            } }.awaitAll()
            scripts.forEachIndexed { index, script ->
                assertEquals(1, script.requests.size)
                assertTrue(script.paths.all { it == (if (index == 0) "/one/" else "/two/") + "chat/completions" })
                assertTrue(script.requests.all { it["model"]?.jsonPrimitive?.content == connections[index].modelId })
            }
        }
    }
}
