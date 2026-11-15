package com.skyd.podaura.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.skyd.fundation.credential.CredentialStore
import com.skyd.podaura.api.model.bean.ArticleQuery
import com.skyd.podaura.api.model.bean.ArticleRecord
import com.skyd.podaura.api.model.bean.ArticleSearchScope
import com.skyd.podaura.api.model.repository.IArticleRepository
import com.skyd.podaura.di.initKoin
import com.skyd.podaura.podnya.capabilities.articles.model.repository.LocalArticleSearchRepository
import com.skyd.podaura.podnya.model.repository.conversation.AssistantConversationRepository
import com.skyd.podaura.podnya.settings.AssistantSettingsViewModel
import com.skyd.podaura.podnya.ui.screen.chat.PodnyaViewModel
import com.skyd.podaura.ui.screen.podnya.PodnyaPages
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.koin.core.context.stopKoin

class PodnyaAssemblyTest {
    @Test
    fun applicationConnectsFeatureToHostPortsAndKeepsOneConversation() = runTest {
        val app = initKoin { modules(podnyaAppModule) }
        val conversationStore = com.skyd.podaura.podnya.model.repository.conversation.InMemoryConversationStore()
        var selectedId: kotlin.uuid.Uuid? = null
        try {
            kotlin.test.assertIs<com.skyd.podaura.podnya.model.repository.conversation.RoomConversationStore>(
                app.koin.get<com.skyd.podaura.podnya.model.repository.conversation.ConversationStore>()
            )
            app.koin.declare<com.skyd.podaura.podnya.model.repository.conversation.ConversationStore>(
                conversationStore
            )
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
            app.koin.declare<IArticleRepository>(object : IArticleRepository {
                override suspend fun query(scope: ArticleSearchScope, query: ArticleQuery) =
                    listOf(ArticleRecord("local-id", title = "<b>Local title</b>"))
            })
            app.koin.declare<com.skyd.podaura.api.model.repository.IFeedRepository>(
                object : com.skyd.podaura.api.model.repository.IFeedRepository {
                    override suspend fun query(query: com.skyd.podaura.api.model.bean.FeedQuery) =
                        emptyList<com.skyd.podaura.api.model.bean.FeedRecord>()
                }
            )
            assertTrue(app.koin.get<PodnyaPages>().available)
            val podnyaCredentials = app.koin.get<com.skyd.fundation.credential.CredentialStore>()
            assertEquals(null, podnyaCredentials.get("podnya-test"))
            val repository = app.koin.get<LocalArticleSearchRepository>()
            assertEquals("Local title", repository.search(ArticleSearchScope.All, ArticleQuery()).items.single().title)
            val conversation = app.koin.get<AssistantConversationRepository>()
            assertSame(conversation, app.koin.get<AssistantConversationRepository>())
            val ready = conversation.state.first { it.storage == com.skyd.podaura.podnya.model.bean.conversation.ConversationStorageState.READY }
            selectedId = ready.selected.id
            conversation.updateDraft(id = ready.selected.id, text = "flush on application close", editorId = "test", revision = 1)
            app.koin.get<PodnyaViewModel>()
            app.koin.get<AssistantSettingsViewModel>()
        } finally {
            stopKoin()
        }
        assertEquals("flush on application close", conversationStore.metadata(selectedId!!)!!.draft)
    }
}
