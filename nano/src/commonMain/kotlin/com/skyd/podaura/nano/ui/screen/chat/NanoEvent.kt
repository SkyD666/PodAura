package com.skyd.podaura.nano.ui.screen.chat

import com.skyd.mvi.MviSingleEvent
import com.skyd.podaura.nano.core.AssistantAction
import com.skyd.podaura.nano.core.AssistantFailure
import kotlin.uuid.Uuid

sealed interface NanoEvent : MviSingleEvent {
    data class ClearDraft(val conversationId: Uuid, val editorId: String, val revision: Long) :
        NanoEvent

    data class Rejected(val conversationId: Uuid, val error: AssistantFailure) : NanoEvent
    data object Back : NanoEvent
    data object OpenSettings : NanoEvent
    data object OpenSearch : NanoEvent
    data class Action(val conversationId: Uuid, val action: AssistantAction) : NanoEvent {
        override fun toString() = "Action([redacted])"
    }
}
