package com.skyd.podaura.podnya.settings

import com.skyd.podaura.podnya.core.normalizeAssistantBaseUrl

internal sealed interface AssistantSettingsPartialStateChange {
    fun reduce(oldState: AssistantSettingsState): AssistantSettingsState

    data class Observed(val saved: AssistantSettings) : AssistantSettingsPartialStateChange {
        override fun reduce(oldState: AssistantSettingsState): AssistantSettingsState {
            if (oldState.loaded && saved == oldState.saved) return oldState
            return if (!oldState.loaded || (!oldState.dirty && oldState.operation == null)) {
                oldState.withSaved(saved)
            } else {
                oldState.copy(
                    saved = saved,
                    pendingSwitch = null,
                    editRevision = oldState.editRevision + 1
                )
            }
        }
    }

    data class Edit(val intent: AssistantSettingsIntent) : AssistantSettingsPartialStateChange {
        override fun reduce(oldState: AssistantSettingsState): AssistantSettingsState {
            if (intent is AssistantSettingsIntent.SetHasNewKey) {
                if (intent.keyRevision != oldState.keyRevision) return oldState
                if (!intent.value) {
                    return oldState.copy(
                        hasNewKey = false,
                        pendingSwitch = null,
                        editRevision = oldState.editRevision + 1,
                    )
                }
            }
            if (!oldState.editable) return oldState
            if (intent == AssistantSettingsIntent.CancelSwitch) return oldState.copy(pendingSwitch = null)
            val target = when (intent) {
                is AssistantSettingsIntent.SelectProvider -> {
                    if (intent.id != AssistantProviderPresets.CUSTOM &&
                        AssistantProviderPresets.find(intent.id) == null
                    ) {
                        return oldState
                    }
                    AssistantProviderSwitch(
                        providerId = intent.id,
                        editRevision = oldState.editRevision
                    )
                }

                is AssistantSettingsIntent.SelectProtocol -> {
                    if (!oldState.custom || intent.protocol == oldState.protocol) return oldState
                    AssistantProviderSwitch(
                        providerId = oldState.providerId!!,
                        protocol = intent.protocol,
                        editRevision = oldState.editRevision,
                    )
                }

                is AssistantSettingsIntent.ConfirmSwitch -> {
                    val pending = oldState.pendingSwitch ?: return oldState
                    if (intent.editRevision != pending.editRevision || intent.editRevision != oldState.editRevision) return oldState
                    return oldState.applySwitch(pending)
                }

                else -> null
            }
            if (target != null) {
                return if (oldState.dirty || oldState.hasNewKey) {
                    oldState.copy(pendingSwitch = target)
                } else oldState.applySwitch(target)
            }
            val edited = when (intent) {
                is AssistantSettingsIntent.SetBaseUrl -> {
                    val value = intent.value
                    val recipient = runCatching {
                        normalizeAssistantBaseUrl(input = value, protocol = oldState.protocol)
                    }.getOrNull()
                    val same = recipient != null && recipient == oldState.normalizedBaseUrl
                    oldState.copy(
                        baseUrl = intent.value,
                        consentAccepted = oldState.consentAccepted && same,
                        hasNewKey = oldState.hasNewKey && same,
                        keyRevision = oldState.keyRevision + if (same) 0 else 1,
                    )
                }

                is AssistantSettingsIntent.SetModelId -> oldState.copy(modelId = intent.value)
                is AssistantSettingsIntent.SetManualModel -> oldState.copy(manualModel = intent.value)
                is AssistantSettingsIntent.SetSessionOnly -> oldState.copy(sessionOnly = intent.value)
                is AssistantSettingsIntent.SetConsentAccepted ->
                    oldState.copy(consentAccepted = intent.value && oldState.normalizedBaseUrl != null)

                is AssistantSettingsIntent.SetHasNewKey -> oldState.copy(hasNewKey = intent.value)
                else -> return oldState
            }
            return edited.copy(
                editRevision = oldState.editRevision + 1,
                pendingSwitch = null,
                failure = null
            )
        }
    }

    data class Started(val operation: AssistantSettingsOperation) :
        AssistantSettingsPartialStateChange {
        override fun reduce(oldState: AssistantSettingsState) = oldState.copy(
            operation = operation, pendingSwitch = null, failure = null,
        )
    }

    data class Finished(
        val saved: AssistantSettings? = null,
        val event: AssistantSettingsEvent? = null,
    ) : AssistantSettingsPartialStateChange {
        override fun reduce(oldState: AssistantSettingsState) =
            (saved?.let { oldState.withSaved(it).copy(savedRevision = oldState.savedRevision + 1) }
                ?: oldState)
                .copy(operation = null, failure = event as? AssistantSettingsEvent.Failed)
    }

    data class Report(val event: AssistantSettingsEvent) : AssistantSettingsPartialStateChange {
        override fun reduce(oldState: AssistantSettingsState) =
            oldState.copy(failure = event as? AssistantSettingsEvent.Failed)
    }
}

private fun AssistantSettingsState.applySwitch(target: AssistantProviderSwitch): AssistantSettingsState {
    val preset = AssistantProviderPresets.find(target.providerId)
    return copy(
        providerId = target.providerId,
        baseUrl = if (target.protocol == null && preset != null) preset.baseUrl else baseUrl,
        modelId = if (target.protocol == null && preset != null) preset.defaultModelId else modelId,
        protocol = target.protocol ?: preset?.protocol ?: protocol,
        manualModel = preset == null,
        consentAccepted = false,
        hasNewKey = false,
        keyRevision = keyRevision + 1,
        editRevision = editRevision + 1,
        pendingSwitch = null,
        failure = null,
    )
}

private fun AssistantSettingsState.withSaved(saved: AssistantSettings): AssistantSettingsState {
    val provider = AssistantProviderPresets.selection(saved)
    return copy(
        saved = saved,
        baseUrl = saved.baseUrl,
        modelId = saved.modelId,
        providerId = provider,
        protocol = saved.protocol,
        manualModel = saved.modelId !in AssistantProviderPresets.find(provider)?.models.orEmpty(),
        sessionOnly = saved.sessionOnly,
        consentAccepted = saved.consentAccepted,
        hasNewKey = false,
        loaded = true,
        editRevision = editRevision + 1,
        keyRevision = keyRevision + 1,
        pendingSwitch = null,
    )
}
