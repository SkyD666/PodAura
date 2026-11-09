package com.skyd.podaura.podnya.settings

internal sealed interface AssistantSettingsPartialStateChange {
    fun reduce(oldState: AssistantSettingsState): AssistantSettingsState

    data class Observed(val saved: AssistantSettings) : AssistantSettingsPartialStateChange {
        override fun reduce(oldState: AssistantSettingsState) =
            if (oldState.loaded) oldState.copy(saved = saved) else oldState.withSaved(saved)
    }

    data class Edit(val intent: AssistantSettingsIntent) : AssistantSettingsPartialStateChange {
        override fun reduce(oldState: AssistantSettingsState): AssistantSettingsState {
            // Disposal must be able to forget a screen-owned key even during verification.
            if (intent == AssistantSettingsIntent.SetHasNewKey(false)) return oldState.copy(
                hasNewKey = false
            )
            if (oldState.operation != null) return oldState
            return when (intent) {
                is AssistantSettingsIntent.SetBaseUrl -> {
                    val value = intent.value
                    val recipient = runCatching { normalizeAssistantBaseUrl(value) }.getOrNull()
                    val sameRecipient = recipient != null && recipient == oldState.normalizedBaseUrl
                    oldState.copy(
                        baseUrl = intent.value,
                        consentAccepted = oldState.consentAccepted && sameRecipient,
                        hasNewKey = oldState.hasNewKey && sameRecipient
                    )
                }

                is AssistantSettingsIntent.SetModelId -> oldState.copy(modelId = intent.value)
                is AssistantSettingsIntent.SetSessionOnly -> oldState.copy(sessionOnly = intent.value)
                is AssistantSettingsIntent.SetConsentAccepted ->
                    oldState.copy(consentAccepted = intent.value && oldState.normalizedBaseUrl != null)

                is AssistantSettingsIntent.SetHasNewKey -> oldState.copy(hasNewKey = intent.value)
                else -> oldState
            }
        }
    }

    data class Started(val operation: AssistantSettingsOperation) :
        AssistantSettingsPartialStateChange {
        override fun reduce(oldState: AssistantSettingsState) = oldState.copy(operation = operation)
    }

    data class Finished(
        val saved: AssistantSettings? = null,
        val event: AssistantSettingsEvent? = null,
    ) : AssistantSettingsPartialStateChange {
        override fun reduce(oldState: AssistantSettingsState) =
            (saved?.let { oldState.withSaved(it).copy(savedRevision = oldState.savedRevision + 1) }
                ?: oldState)
                .copy(operation = null)
    }

    data class Report(val event: AssistantSettingsEvent) : AssistantSettingsPartialStateChange {
        override fun reduce(oldState: AssistantSettingsState) = oldState
    }
}

private fun AssistantSettingsState.withSaved(saved: AssistantSettings) = copy(
    saved = saved,
    baseUrl = saved.baseUrl,
    modelId = saved.modelId,
    sessionOnly = saved.sessionOnly,
    consentAccepted = saved.consentAccepted,
    hasNewKey = false,
    loaded = true,
)
