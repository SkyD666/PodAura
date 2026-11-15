package com.skyd.podaura.podnya.settings

import com.skyd.mvi.MviIntent
import com.skyd.podaura.podnya.core.AssistantProtocol

sealed interface AssistantSettingsIntent : MviIntent {
    data class SetBaseUrl(val value: String) : AssistantSettingsIntent {
        override fun toString() = "SetBaseUrl([redacted])"
    }

    data class SetModelId(val value: String) : AssistantSettingsIntent {
        override fun toString() = "SetModelId([redacted])"
    }

    data class SetSessionOnly(val value: Boolean) : AssistantSettingsIntent
    data class SetConsentAccepted(val value: Boolean) : AssistantSettingsIntent
    data class SetHasNewKey(val value: Boolean, val keyRevision: Long) : AssistantSettingsIntent

    // MVI logs every intent. Never let a credential enter generated data-class output.
    class Save(
        val apiKey: String?,
        val editRevision: Long,
        val keyRevision: Long
    ) : AssistantSettingsIntent {
        override fun toString() = "Save([redacted])"
    }

    data class SelectProvider(val id: String) : AssistantSettingsIntent
    data class SelectProtocol(val protocol: AssistantProtocol) : AssistantSettingsIntent
    data class ConfirmSwitch(val editRevision: Long) : AssistantSettingsIntent
    data object CancelSwitch : AssistantSettingsIntent
    data class SetManualModel(val value: Boolean) : AssistantSettingsIntent
    data object Reload : AssistantSettingsIntent
    data object Verify : AssistantSettingsIntent
    data object Clear : AssistantSettingsIntent
    data object CancelVerification : AssistantSettingsIntent
}
