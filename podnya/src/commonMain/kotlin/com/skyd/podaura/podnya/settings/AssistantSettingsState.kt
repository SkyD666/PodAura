package com.skyd.podaura.podnya.settings

import com.skyd.mvi.MviViewState
import com.skyd.podaura.podnya.core.AssistantProtocol
import com.skyd.podaura.podnya.core.normalizeAssistantBaseUrl

enum class AssistantSettingsOperation { SAVING, TESTING, CLEARING }

data class AssistantProviderSwitch(
    val providerId: String,
    val protocol: AssistantProtocol? = null,
    val editRevision: Long,
)

/** Credentials live only in the screen and the repository, never in view state. */
data class AssistantSettingsState(
    val available: Boolean = false,
    val saved: AssistantSettings = AssistantSettings(),
    val baseUrl: String = "",
    val modelId: String = "",
    val sessionOnly: Boolean = false,
    val consentAccepted: Boolean = false,
    val hasNewKey: Boolean = false,
    val loaded: Boolean = false,
    val operation: AssistantSettingsOperation? = null,
    val savedRevision: Int = 0,
    val providerId: String? = null,
    val protocol: AssistantProtocol = AssistantProtocol.OPENAI_COMPATIBLE,
    val manualModel: Boolean = false,
    val editRevision: Long = 0,
    val keyRevision: Long = 0,
    val pendingSwitch: AssistantProviderSwitch? = null,
    val failure: AssistantSettingsEvent.Failed? = null,
) : MviViewState {
    val normalizedBaseUrl: String?
        get() = runCatching {
            normalizeAssistantBaseUrl(
                input = baseUrl,
                protocol = protocol
            )
        }.getOrNull()
    val sameRecipient: Boolean
        get() = normalizedBaseUrl != null && normalizedBaseUrl == saved.baseUrl && protocol == saved.protocol
    val dirty: Boolean
        get() = (normalizedBaseUrl
            ?: baseUrl.trim()) != saved.baseUrl || modelId.trim() != saved.modelId ||
                providerId != AssistantProviderPresets.selection(saved) || protocol != saved.protocol ||
                sessionOnly != saved.sessionOnly || consentAccepted != saved.consentAccepted || hasNewKey
    val editable: Boolean get() = loaded && operation == null && saved.readError == null && available
    val custom: Boolean get() = providerId != null && AssistantProviderPresets.find(providerId) == null
    val canVerify: Boolean
        get() = loaded && operation == null && !dirty && saved.configured && saved.consentAccepted

    override fun toString() = "AssistantSettingsState(loaded=$loaded, operation=$operation, " +
            "hasNewKey=$hasNewKey, savedRevision=$savedRevision)"
}
