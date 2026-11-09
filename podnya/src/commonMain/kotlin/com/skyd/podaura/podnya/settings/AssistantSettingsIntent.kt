package com.skyd.podaura.podnya.settings

import com.skyd.mvi.MviIntent

sealed interface AssistantSettingsIntent : MviIntent {
    data class SetBaseUrl(val value: String) : AssistantSettingsIntent {
        override fun toString() = "SetBaseUrl([redacted])"
    }

    data class SetModelId(val value: String) : AssistantSettingsIntent {
        override fun toString() = "SetModelId([redacted])"
    }

    data class SetSessionOnly(val value: Boolean) : AssistantSettingsIntent
    data class SetConsentAccepted(val value: Boolean) : AssistantSettingsIntent
    data class SetHasNewKey(val value: Boolean) : AssistantSettingsIntent

    // MVI logs every intent. Never let a credential enter generated data-class output.
    class Save(val apiKey: String?) : AssistantSettingsIntent {
        override fun toString() = "Save([redacted])"
    }

    data object Verify : AssistantSettingsIntent
    data object Clear : AssistantSettingsIntent
    data object CancelVerification : AssistantSettingsIntent
}
