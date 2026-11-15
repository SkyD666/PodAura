package com.skyd.podaura.podnya.ui.screen.chat

import com.skyd.mvi.MviSingleEvent
import com.skyd.podaura.podnya.core.AssistantAction
import com.skyd.podaura.podnya.core.AssistantFailure
import com.skyd.podaura.podnya.model.repository.conversation.AssistantConversationRepository.Confirmation
import kotlin.uuid.Uuid

sealed interface PodnyaEvent : MviSingleEvent {
    data class ClearDraft(val conversationId: Uuid, val editorId: String, val revision: Long) :
        PodnyaEvent

    data class Rejected(val conversationId: Uuid, val error: AssistantFailure) : PodnyaEvent
    data class ConfirmDestination(val confirmation: Confirmation) : PodnyaEvent
    data object Back : PodnyaEvent
    data object OpenSettings : PodnyaEvent
    data object OpenSearch : PodnyaEvent
    data class Action(val conversationId: Uuid, val action: AssistantAction) : PodnyaEvent {
        override fun toString() = "Action([redacted])"
    }
}
