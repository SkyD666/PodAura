package com.skyd.podaura.podnya.settings

import com.skyd.mvi.MviIntent
import com.skyd.podaura.podnya.core.AssistantProtocol
import com.skyd.podaura.podnya.core.AssistantReasoningEffort

sealed interface AssistantSettingsIntent : MviIntent {
    data class SetBaseUrl(val value: String) : AssistantSettingsIntent {
        override fun toString() = "SetBaseUrl([redacted])"
    }

    data class SetModelId(val value: String) : AssistantSettingsIntent {
        override fun toString() = "SetModelId([redacted])"
    }

    data class SetReasoningEffort(val value: AssistantReasoningEffort) : AssistantSettingsIntent
    data class SetHasNewKey(val value: Boolean, val keyRevision: Long) : AssistantSettingsIntent

    // MVI logs every intent. Never let a credential enter generated data-class output.
    class Save(
        val apiKey: String?,
        val editRevision: Long,
        val keyRevision: Long,
        val activate: Boolean = false,
    ) : AssistantSettingsIntent {
        override fun toString() = "Save([redacted])"
    }

    data class SelectProvider(val id: String) : AssistantSettingsIntent
    data class SelectProtocol(val protocol: AssistantProtocol) : AssistantSettingsIntent
    data class SetManualModel(val value: Boolean) : AssistantSettingsIntent
    data object DiscardConflictingEdits : AssistantSettingsIntent
    data object Reload : AssistantSettingsIntent
    data object Verify : AssistantSettingsIntent
    data object CloseEditor : AssistantSettingsIntent
    data class SelectProfile(val id: String) : AssistantSettingsIntent
    data class EditProfile(val id: String?) : AssistantSettingsIntent
    data class SetName(val value: String) : AssistantSettingsIntent {
        override fun toString() = "SetName([redacted])"
    }

    data class Clear(val profileId: String?, val replacementId: String? = null) :
        AssistantSettingsIntent

    data object CancelVerification : AssistantSettingsIntent
}
