package com.skyd.podaura.nano.settings

import com.skyd.mvi.MviSingleEvent
import com.skyd.podaura.nano.core.AssistantFailure

sealed interface AssistantSettingsEvent : MviSingleEvent {
    data object Saved : AssistantSettingsEvent
    data object Cleared : AssistantSettingsEvent
    data object ConnectionSucceeded : AssistantSettingsEvent
    data class Failed(val error: AssistantFailure? = null) : AssistantSettingsEvent
}
