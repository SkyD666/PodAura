package com.skyd.podaura.podnya.ui.screen.chat

import com.skyd.mvi.MviIntent
import com.skyd.podaura.podnya.core.AssistantAction

sealed interface PodnyaIntent : MviIntent {
    // AbstractMviViewModel logs intents even when debugLog() is not used.
    data class UpdateDraft(val text: String) : PodnyaIntent {
        override fun toString() = "UpdateDraft([redacted])"
    }

    data class Send(val editorId: String, val revision: Long) : PodnyaIntent
    data object Retry : PodnyaIntent
    data object Stop : PodnyaIntent
    data class NewConversation(val editorId: String, val revision: Long) : PodnyaIntent
    data object Back : PodnyaIntent
    data object OpenSettings : PodnyaIntent
    data object OpenSearch : PodnyaIntent
    data class PerformAction(val action: AssistantAction) : PodnyaIntent {
        override fun toString() = "PerformAction([redacted])"
    }
}
