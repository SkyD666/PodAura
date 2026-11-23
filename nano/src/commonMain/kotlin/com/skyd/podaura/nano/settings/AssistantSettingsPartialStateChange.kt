package com.skyd.podaura.nano.settings

import com.skyd.podaura.nano.core.AssistantReasoningEffort
import com.skyd.podaura.nano.core.normalizeAssistantBaseUrl

internal sealed interface AssistantSettingsPartialStateChange {
    fun reduce(oldState: AssistantSettingsState): AssistantSettingsState

    data class Observed(val profiles: AssistantProfiles) : AssistantSettingsPartialStateChange {
        override fun reduce(oldState: AssistantSettingsState): AssistantSettingsState {
            val state = oldState.copy(profiles = profiles)
            val saved = profiles.profiles.find { it.configId == state.saved.configId }
            return when {
                !state.loaded || profiles.readError != null -> state.withSaved(profiles.active)
                    .copy(editing = false)

                state.operation != null -> state
                state.editing && state.saved.configId == null -> state
                state.saved.configId == null && state.saved.readError == null &&
                        (oldState.profiles.profiles.isNotEmpty() || state.dirty) -> state

                saved == null -> state.withSaved(profiles.active).copy(editing = false)
                saved != state.saved && !state.dirty -> state.withSaved(saved)
                else -> state
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
                        editRevision = oldState.editRevision + 1,
                    )
                }
            }
            if (intent == AssistantSettingsIntent.CloseEditor) {
                return oldState.withSaved(oldState.profiles.active).copy(editing = false)
            }
            if (!oldState.editable) return oldState
            if (intent == AssistantSettingsIntent.DiscardConflictingEdits) {
                if (!oldState.hasEditConflict) return oldState
                return oldState.withSaved(requireNotNull(oldState.latestSaved))
            }
            if (intent is AssistantSettingsIntent.EditProfile) {
                if (oldState.editing && intent.id == oldState.saved.configId) return oldState
                val profile = if (intent.id == null) {
                    AssistantSettings()
                } else {
                    oldState.profiles.profiles.find { it.configId == intent.id } ?: return oldState
                }
                return oldState.withSaved(profile).copy(editing = true)
            }
            val target = when (intent) {
                is AssistantSettingsIntent.SelectProvider -> {
                    if (intent.id == AssistantProviderPresets.CUSTOM && oldState.custom) return oldState
                    if (intent.id != AssistantProviderPresets.CUSTOM &&
                        AssistantProviderPresets.find(intent.id) == null
                    ) {
                        return oldState
                    }
                    AssistantProviderSwitch(providerId = intent.id)
                }

                is AssistantSettingsIntent.SelectProtocol -> {
                    if (!oldState.custom || intent.protocol == oldState.protocol) return oldState
                    AssistantProviderSwitch(
                        providerId = oldState.providerId!!,
                        protocol = intent.protocol,
                    )
                }

                else -> null
            }
            if (target != null) {
                return oldState.applySwitch(target)
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
                        hasNewKey = oldState.hasNewKey && same,
                        keyRevision = oldState.keyRevision + if (same) 0 else 1,
                    )
                }

                is AssistantSettingsIntent.SetModelId -> oldState.copy(
                    modelId = intent.value,
                    name = oldState.defaultNameFor(intent.value),
                )

                is AssistantSettingsIntent.SetReasoningEffort -> {
                    if (!oldState.supportsReasoningEffort) return oldState
                    oldState.copy(reasoningEffort = intent.value, reasoningEffortReset = false)
                }

                is AssistantSettingsIntent.SetManualModel -> oldState.copy(manualModel = intent.value)
                is AssistantSettingsIntent.SetName -> oldState.copy(name = intent.value)
                is AssistantSettingsIntent.SetHasNewKey -> oldState.copy(hasNewKey = intent.value)
                else -> return oldState
            }
            return edited.normalizeReasoningEffort(oldState).copy(
                editRevision = oldState.editRevision + 1,
                failure = null
            )
        }
    }

    data class Started(val operation: AssistantSettingsOperation) :
        AssistantSettingsPartialStateChange {
        override fun reduce(oldState: AssistantSettingsState) = oldState.copy(
            operation = operation, failure = null,
        )
    }

    data class Finished(
        val saved: AssistantSettings? = null,
        val event: AssistantSettingsEvent? = null,
        val testedProfile: AssistantSettings? = null,
    ) : AssistantSettingsPartialStateChange {
        fun forProfiles(catalog: AssistantProfiles): Finished {
            val tested = testedProfile ?: return this
            val current = catalog.profiles.find { it.configId == tested.configId }
            return if (current?.revision == tested.revision) this else copy(event = null)
        }

        override fun reduce(oldState: AssistantSettingsState): AssistantSettingsState {
            val idle =
                oldState.copy(operation = null, failure = event as? AssistantSettingsEvent.Failed)
            return if (saved != null) {
                idle.withSaved(saved).copy(editing = false, savedRevision = idle.savedRevision + 1)
            } else {
                Observed(idle.profiles).reduce(idle)
            }
        }
    }

    data class Report(val event: AssistantSettingsEvent) : AssistantSettingsPartialStateChange {
        override fun reduce(oldState: AssistantSettingsState) =
            oldState.copy(failure = event as? AssistantSettingsEvent.Failed)
    }
}

private fun AssistantSettingsState.applySwitch(target: AssistantProviderSwitch): AssistantSettingsState {
    val preset = AssistantProviderPresets.find(target.providerId)
    val model = preset?.defaultModelId ?: modelId
    val updated = copy(
        providerId = target.providerId,
        baseUrl = if (target.protocol == null && preset != null) preset.baseUrl else baseUrl,
        modelId = model,
        name = defaultNameFor(model),
        protocol = target.protocol ?: preset?.protocol ?: protocol,
        manualModel = preset == null,
        editRevision = editRevision + 1,
        failure = null,
    ).normalizeReasoningEffort(this)
    val sameRecipient = normalizedBaseUrl != null &&
            updated.normalizedBaseUrl == normalizedBaseUrl &&
            updated.protocol == protocol
    return if (sameRecipient) updated else updated.copy(
        hasNewKey = false,
        keyRevision = keyRevision + 1
    )
}

private fun AssistantSettingsState.defaultNameFor(model: String): String =
    if (saved.configId == null && (name.isBlank() || name == modelId)) model else name

private fun AssistantSettingsState.normalizeReasoningEffort(previous: AssistantSettingsState): AssistantSettingsState =
    if (supportsReasoningEffort != previous.supportsReasoningEffort) {
        copy(
            reasoningEffort = AssistantReasoningEffort.LOW,
            reasoningEffortReset = !supportsReasoningEffort,
        )
    } else {
        this
    }

private fun AssistantSettingsState.withSaved(saved: AssistantSettings): AssistantSettingsState {
    val provider = AssistantProviderPresets.selection(saved)
    return copy(
        saved = saved,
        name = saved.name,
        failure = null,
        baseUrl = saved.baseUrl,
        modelId = saved.modelId,
        reasoningEffort = saved.reasoningEffort,
        reasoningEffortReset = false,
        providerId = provider,
        protocol = saved.protocol,
        manualModel = saved.modelId !in AssistantProviderPresets.find(provider)?.models.orEmpty(),
        hasNewKey = false,
        loaded = true,
        editRevision = editRevision + 1,
        keyRevision = keyRevision + 1,
    )
}
