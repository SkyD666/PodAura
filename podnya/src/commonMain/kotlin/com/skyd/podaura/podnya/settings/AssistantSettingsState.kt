package com.skyd.podaura.podnya.settings

import com.skyd.mvi.MviViewState

enum class AssistantSettingsOperation { SAVING, TESTING, CLEARING }

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
) : MviViewState {
    val normalizedBaseUrl: String?
        get() = runCatching { normalizeAssistantBaseUrl(baseUrl) }.getOrNull()
    val sameRecipient: Boolean
        get() = normalizedBaseUrl != null && normalizedBaseUrl == saved.baseUrl
    val dirty: Boolean
        get() = normalizedBaseUrl != saved.baseUrl || modelId.trim() != saved.modelId ||
                sessionOnly != saved.sessionOnly || consentAccepted != saved.consentAccepted || hasNewKey
    val canVerify: Boolean
        get() = loaded && operation == null && !dirty && saved.configured && saved.consentAccepted

    override fun toString() = "AssistantSettingsState(loaded=$loaded, operation=$operation, " +
            "hasNewKey=$hasNewKey, savedRevision=$savedRevision)"
}
