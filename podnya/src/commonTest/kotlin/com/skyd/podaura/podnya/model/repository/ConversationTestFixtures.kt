package com.skyd.podaura.podnya.model.repository

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
        it.save("https://first.example/v1", "first-model", "test-key", false, true)
    }
internal suspend fun AssistantConversationRepository.editDraft(text: String) {
    val current = state.value.selected
    updateDraft(current.id, text, "test-editor", current.draftRevision + 1)
}
internal suspend fun AssistantConversationRepository.sendDraft(settings: AssistantSettingsRepository): Boolean {
    val current = state.value.selected
    val config = settings.snapshot()
    return submit(current.id, current.editorId, current.draftRevision, config.configId, config.revision)
}
internal suspend fun AssistantConversationRepository.retryLast(settings: AssistantSettingsRepository): Boolean {
    val current = state.value.selected
    val config = settings.snapshot()
    return submit(current.id, current.editorId, current.draftRevision, config.configId, config.revision, current.turns.last().id)
}
