package com.skyd.podaura.podnya.capabilities.articles.model.bean

import com.skyd.podaura.api.model.bean.ArticleSearchScope
import com.skyd.podaura.podnya.core.AssistantConnectionResult
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantRunEvent
import com.skyd.podaura.podnya.core.AssistantRuntime
import com.skyd.podaura.podnya.core.AssistantTurnRequest
import com.skyd.podaura.podnya.model.repository.AssistantConversationRepository
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
            override fun run(request: AssistantTurnRequest) = flowOf<AssistantRunEvent>(
                AssistantRunEvent.Failed(AssistantError.NETWORK),
            ).also { requests += request }
        }
        val repository = AssistantConversationRepository(runtime, backgroundScope)
        repository.newConversation(context)
        feeds.clear()
        articles.clear()
        feeds += "different-feed"
        articles += "different-article"

        assertTrue(repository.submit("Find related articles"))
        runCurrent()
        assertTrue(repository.retry())
        runCurrent()

        assertEquals(2, requests.size)
        requests.forEach { assertEquals(expected, assertIs<ArticleSearchContext>(it.context).scope) }
        assertEquals(expected, assertIs<ArticleSearchContext>(repository.state.value.context).scope)
        assertEquals(expected, assertIs<ArticleSearchContext>(repository.state.value.turns.single().context).scope)
    }
}
