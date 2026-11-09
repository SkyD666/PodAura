package com.skyd.podaura.podnya.settings

import com.skyd.mvi.MviSingleEvent
import com.skyd.podaura.podnya.core.AssistantFailure

sealed interface AssistantSettingsEvent : MviSingleEvent {
    data object Saved : AssistantSettingsEvent
    data object Cleared : AssistantSettingsEvent
    data object ConnectionSucceeded : AssistantSettingsEvent
    data class Failed(val error: AssistantFailure? = null) : AssistantSettingsEvent
}
