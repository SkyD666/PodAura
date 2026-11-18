package com.skyd.podaura.podnya.model.repository.conversation

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.skyd.fundation.credential.CredentialStore
import com.skyd.podaura.podnya.settings.AssistantSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow

internal class MemoryConversationPreferences : DataStore<Preferences> {
    override val data = MutableStateFlow(emptyPreferences())
    override suspend fun updateData(transform: suspend (Preferences) -> Preferences) =
        transform(data.value).also { data.value = it }
}
internal open class ConversationCredentials : CredentialStore {
    private val keys = mutableMapOf<String, String>()
    override suspend fun get(id: String) = keys[id]
    override suspend fun put(id: String, secret: String) { keys[id] = secret }
    override suspend fun delete(id: String) { keys.remove(id) }
}
internal suspend fun conversationSettings(credentials: CredentialStore = ConversationCredentials()) =
    AssistantSettingsRepository(MemoryConversationPreferences(), credentials).also {
        it.save(baseUrl = "https://first.example/v1", modelId = "first-model", apiKey = "test-key")
    }
internal suspend fun AssistantConversationRepository.editDraft(text: String) {
    val current = state.value.selected
    updateDraft(current.id, text, "test-editor", current.draftRevision + 1)
}
internal suspend fun AssistantConversationRepository.sendDraft(): Boolean {
    val current = state.value.selected
    return submit(id = current.id, editorId = current.editorId, revision = current.draftRevision)
}
internal suspend fun AssistantConversationRepository.retryLast(): Boolean {
    val current = state.value.selected
    return submit(
        id = current.id,
        editorId = current.editorId,
        revision = current.draftRevision,
        retryTurnId = current.turns.last().id,
    )
}

/** Test-only full read, never used by production or UI. */
internal suspend fun ConversationStore.get(id: kotlin.uuid.Uuid): com.skyd.podaura.podnya.model.bean.conversation.AssistantSessionState? =
    metadata(id)?.copy(turns = tail(id = id, limit = 10_000))
