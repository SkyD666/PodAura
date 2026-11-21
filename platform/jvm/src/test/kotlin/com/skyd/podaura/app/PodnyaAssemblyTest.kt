package com.skyd.podaura.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.skyd.fundation.credential.CredentialStore
import com.skyd.podaura.api.model.bean.article.ArticleQuery
import com.skyd.podaura.api.model.bean.article.ArticleRecord
import com.skyd.podaura.api.model.bean.article.ArticleSearchScope
import com.skyd.podaura.api.model.bean.download.AssistantDownloadArticle
import com.skyd.podaura.api.model.bean.download.AssistantDownloadReceipt
import com.skyd.podaura.api.model.bean.download.AssistantDownloadScope
import com.skyd.podaura.api.model.bean.download.AssistantDownloadTarget
import com.skyd.podaura.api.model.bean.feed.FeedQuery
import com.skyd.podaura.api.model.bean.feed.FeedRecord
import com.skyd.podaura.api.model.repository.IArticleRepository
import com.skyd.podaura.api.model.repository.IAssistantDownloadRepository
import com.skyd.podaura.api.model.repository.IFeedRepository
import com.skyd.podaura.di.initKoin
import com.skyd.podaura.podnya.capabilities.articles.model.repository.LocalArticleSearchRepository
import com.skyd.podaura.podnya.model.bean.conversation.ConversationStorageState
import com.skyd.podaura.podnya.model.repository.conversation.AssistantConversationRepository
import com.skyd.podaura.podnya.model.repository.conversation.ConversationStore
import com.skyd.podaura.podnya.model.repository.conversation.InMemoryConversationStore
import com.skyd.podaura.podnya.model.repository.conversation.RoomConversationStore
import com.skyd.podaura.podnya.settings.AssistantSettingsViewModel
import com.skyd.podaura.podnya.ui.screen.chat.PodnyaViewModel
import com.skyd.podaura.ui.screen.podnya.PodnyaPages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.koin.core.context.stopKoin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class PodnyaAssemblyTest {
    @Test
    fun applicationConnectsFeatureToHostPortsAndKeepsOneConversation() = runTest {
        val app = initKoin { modules(podnyaAppModule) }
        val conversationStore = InMemoryConversationStore()
        var selectedId: Uuid? = null
        try {
            assertIs<RoomConversationStore>(app.koin.get<ConversationStore>())
            app.koin.declare<ConversationStore>(conversationStore)
            // Replace storage before resolving the graph; never open the user's database or vault.
            app.koin.declare<DataStore<Preferences>>(object : DataStore<Preferences> {
                override val data = MutableStateFlow(emptyPreferences())
                override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                    transform(data.value).also { data.value = it }
            })
            app.koin.declare<CredentialStore>(object : CredentialStore {
                override suspend fun put(id: String, secret: String) = Unit
                override suspend fun get(id: String): String? = null
                override suspend fun delete(id: String) = Unit
            })
            app.koin.declare<IArticleRepository>(TestArticles)
            app.koin.declare<IFeedRepository>(TestFeeds)
            app.koin.declare<IAssistantDownloadRepository>(TestDownloads())
            assertTrue(app.koin.get<PodnyaPages>().available)
            val podnyaCredentials = app.koin.get<CredentialStore>()
            assertEquals(null, podnyaCredentials.get("podnya-test"))
            val repository = app.koin.get<LocalArticleSearchRepository>()
            assertEquals("Local title", repository.search(ArticleSearchScope.All, ArticleQuery()).items.single().title)
            val conversation = app.koin.get<AssistantConversationRepository>()
            assertSame(conversation, app.koin.get<AssistantConversationRepository>())
            val ready = conversation.state.first { it.storage == ConversationStorageState.READY }
            selectedId = ready.selected.id
            conversation.updateDraft(id = ready.selected.id, text = "flush on application close", editorId = "test", revision = 1)
            app.koin.get<PodnyaViewModel>()
            app.koin.get<AssistantSettingsViewModel>()
        } finally {
            stopKoin()
        }
        assertEquals("flush on application close", conversationStore.metadata(checkNotNull(selectedId))!!.draft)
    }
}

internal object TestArticles : IArticleRepository {
    override suspend fun query(scope: ArticleSearchScope, query: ArticleQuery) =
        listOf(ArticleRecord(articleId = "local-id", title = "<b>Local title</b>"))
            .filter { query.withinArticleIds?.contains(it.articleId) != false }
}

internal object TestFeeds : IFeedRepository {
    override suspend fun query(query: FeedQuery) = listOf(
        FeedRecord(
            url = "https://feed.test/",
            title = "Saved feed",
            nickname = null,
            description = null,
            customDescription = null,
            muted = false,
        ),
    ).filter { query.withinFeedUrls?.contains(it.url) != false }
}

internal class TestDownloads : IAssistantDownloadRepository {
    var checkedLink: String? = null

    override suspend fun articlesByIds(articleIds: List<String>): List<AssistantDownloadArticle> =
        error("A direct link does not require articles")

    override suspend fun articlesInFeed(scope: AssistantDownloadScope): List<AssistantDownloadArticle> =
        error("A direct link does not require a subscription")

    override suspend fun checkDirectLink(url: String): Boolean {
        checkedLink = url
        return url == "https://media.test/file.mp3"
    }

    override suspend fun submit(
        batchId: String,
        submissionId: String,
        targets: List<AssistantDownloadTarget>,
    ): AssistantDownloadReceipt = error("A model turn may only prepare a plan")

    override suspend fun receipt(batchId: String, submissionId: String): AssistantDownloadReceipt? = null
}
