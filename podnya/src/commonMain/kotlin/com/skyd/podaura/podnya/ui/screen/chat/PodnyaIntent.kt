package com.skyd.podaura.podnya.ui.screen.chat

import com.skyd.mvi.MviIntent
import com.skyd.podaura.podnya.core.AssistantAction
import com.skyd.podaura.podnya.model.repository.AssistantConversationRepository.Confirmation
import kotlin.uuid.Uuid

sealed interface PodnyaIntent : MviIntent {
    data class UpdateDraft(
        val conversationId: Uuid,
        val text: String,
        val editorId: String,
        val revision: Long
    ) : PodnyaIntent {
        override fun toString() = "UpdateDraft($conversationId, [redacted])"
    }

    data class Send(
        val conversationId: Uuid,
        val editorId: String,
        val revision: Long,
        val configId: String?,
        val configRevision: Long
    ) : PodnyaIntent

    data class Retry(
        val conversationId: Uuid,
        val turnId: Long,
        val editorId: String,
        val revision: Long,
        val configId: String?,
        val configRevision: Long
    ) : PodnyaIntent

    data class Stop(val conversationId: Uuid) : PodnyaIntent
    data object NewConversation : PodnyaIntent
    data class Select(val conversationId: Uuid) : PodnyaIntent
    data class Rename(val conversationId: Uuid, val title: String) : PodnyaIntent {
        override fun toString() = "Rename($conversationId, [redacted])"
    }

    data class Delete(val conversationId: Uuid, val stopReplyConfirmed: Boolean) : PodnyaIntent
    data class SelectModel(val conversationId: Uuid, val configId: String) : PodnyaIntent
    data class ConfirmDestination(val confirmation: Confirmation) : PodnyaIntent
    data object Back : PodnyaIntent
    data object OpenSettings : PodnyaIntent
    data object OpenSearch : PodnyaIntent
    data class PerformAction(val conversationId: Uuid, val action: AssistantAction) : PodnyaIntent {
        override fun toString() = "PerformAction($conversationId, [redacted])"
    }
}
