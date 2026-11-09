package com.skyd.podaura.podnya.ui.screen.chat

import com.skyd.mvi.MviSingleEvent
import com.skyd.podaura.podnya.core.AssistantAction

sealed interface PodnyaEvent : MviSingleEvent {
    data class ClearDraft(val editorId: String, val revision: Long) : PodnyaEvent
    data object Back : PodnyaEvent
    data object OpenSettings : PodnyaEvent
    data object OpenSearch : PodnyaEvent
    data class Action(val action: AssistantAction) : PodnyaEvent {
        override fun toString() = "Action([redacted])"
    }
}
