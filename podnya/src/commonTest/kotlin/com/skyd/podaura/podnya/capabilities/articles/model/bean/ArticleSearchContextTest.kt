package com.skyd.podaura.podnya.capabilities.articles.model.bean

import com.skyd.podaura.api.model.bean.ArticleSearchScope
import com.skyd.podaura.podnya.core.AssistantConnectionResult
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantRunEvent
import com.skyd.podaura.podnya.core.AssistantRuntime
import com.skyd.podaura.podnya.core.AssistantTurnRequest
import com.skyd.podaura.podnya.model.repository.conversation.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ArticleSearchContextTest {
    @Test
    fun conversationAndRetryKeepTheOriginalScopeWhenSelectionsChange() = runTest {
        val feeds = mutableSetOf("feed-a", "feed-b")
        val articles = mutableSetOf("article-a", "article-b")
        val expected = ArticleSearchScope.Restricted(feeds.toSet(), articles.toSet())
        val context = ArticleSearchContext(ArticleSearchScope.Restricted(feeds, articles))
        val requests = mutableListOf<AssistantTurnRequest>()
        val runtime = object : AssistantRuntime {
            override val available = true
            override suspend fun verify() = AssistantConnectionResult(true)
            override fun run(request: AssistantTurnRequest, connection: com.skyd.podaura.podnya.core.AssistantConnection) = flowOf<AssistantRunEvent>(
                AssistantRunEvent.Failed(AssistantError.NETWORK),
            ).also { requests += request }
        }
        val settings = conversationSettings()
        val repository = AssistantConversationRepository(runtime, settings, scope = backgroundScope)
        repository.newConversation(context)
        feeds.clear()
        articles.clear()
        feeds += "different-feed"
        articles += "different-article"

        repository.editDraft("Find related articles")
        assertTrue(repository.sendDraft(settings))
        runCurrent()
        assertTrue(repository.retryLast(settings))
        runCurrent()

        assertEquals(2, requests.size)
        requests.forEach { assertEquals(expected, assertIs<ArticleSearchContext>(it.context).scope) }
        assertEquals(expected, assertIs<ArticleSearchContext>(repository.state.value.selected.context).scope)
        assertEquals(expected, assertIs<ArticleSearchContext>(repository.state.value.selected.turns.single().context).scope)
    }
}
