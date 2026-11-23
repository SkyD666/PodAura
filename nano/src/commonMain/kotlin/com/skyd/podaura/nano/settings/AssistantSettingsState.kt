package com.skyd.podaura.nano.settings

import com.skyd.mvi.MviViewState
import com.skyd.podaura.nano.core.AssistantProtocol
import com.skyd.podaura.nano.core.AssistantReasoningEffort
import com.skyd.podaura.nano.core.normalizeAssistantBaseUrl
import com.skyd.podaura.nano.core.supportsAssistantReasoningEffort

enum class AssistantSettingsOperation { SAVING, TESTING, CLEARING }

data class AssistantProviderSwitch(
    val providerId: String,
    val protocol: AssistantProtocol? = null,
)

/** Credentials live only in the screen and the repository, never in view state. */
data class AssistantSettingsState(
    val available: Boolean = false,
    val editing: Boolean = false,
    val saved: AssistantSettings = AssistantSettings(),
    val baseUrl: String = "",
    val modelId: String = "",
    val reasoningEffort: AssistantReasoningEffort = AssistantReasoningEffort.LOW,
    val reasoningEffortReset: Boolean = false,
    val profiles: AssistantProfiles = AssistantProfiles(),
    val name: String = "",
    val hasNewKey: Boolean = false,
    val loaded: Boolean = false,
    val operation: AssistantSettingsOperation? = null,
    val savedRevision: Int = 0,
    val providerId: String? = null,
    val protocol: AssistantProtocol = AssistantProtocol.OPENAI_COMPATIBLE,
    val manualModel: Boolean = false,
    val editRevision: Long = 0,
    val keyRevision: Long = 0,
    val failure: AssistantSettingsEvent.Failed? = null,
) : MviViewState {
    val latestSaved: AssistantSettings? get() = profiles.profiles.find { it.configId == saved.configId }
    val hasEditConflict: Boolean get() = latestSaved?.revision?.let { it != saved.revision } == true
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
                reasoningEffort != saved.reasoningEffort ||
                name != saved.name || hasNewKey
    val supportsReasoningEffort: Boolean
        get() = supportsAssistantReasoningEffort(
            baseUrl = normalizedBaseUrl ?: "",
            modelId = modelId,
            protocol = protocol,
        )
    val editable: Boolean get() = loaded && operation == null && saved.readError == null && available
    val custom: Boolean get() = providerId != null && AssistantProviderPresets.find(providerId) == null
    val canSave: Boolean
        get() = editable && dirty && !hasEditConflict && providerId != null && normalizedBaseUrl != null &&
                modelId.isNotBlank() && (hasNewKey || (sameRecipient && saved.hasApiKey))
    val canVerify: Boolean
        get() = loaded && operation == null && !dirty && saved.configured

    override fun toString() = "AssistantSettingsState(loaded=$loaded, operation=$operation, " +
            "hasNewKey=$hasNewKey, savedRevision=$savedRevision)"
}
