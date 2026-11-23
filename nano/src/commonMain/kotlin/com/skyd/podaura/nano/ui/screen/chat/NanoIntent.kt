package com.skyd.podaura.nano.ui.screen.chat

import com.skyd.mvi.MviIntent
import com.skyd.podaura.nano.core.AssistantAction
import com.skyd.podaura.nano.model.bean.conversation.ConversationPosition
import kotlin.uuid.Uuid

sealed interface NanoIntent : MviIntent {
    data class UpdateDraft(
        val conversationId: Uuid,
        val text: String,
        val editorId: String,
        val revision: Long,
        val selectionStart: Int = text.length,
        val selectionEnd: Int = selectionStart,
    ) : NanoIntent {
        override fun toString() = "UpdateDraft($conversationId, [redacted])"
    }

    data class Send(
        val conversationId: Uuid,
        val editorId: String,
        val revision: Long,
    ) : NanoIntent

    data class Retry(
        val conversationId: Uuid,
        val turnId: Long,
        val editorId: String,
        val revision: Long,
    ) : NanoIntent

    data class Stop(val conversationId: Uuid) : NanoIntent
    data object NewConversation : NanoIntent
    data class Select(val conversationId: Uuid) : NanoIntent
    data class Rename(val conversationId: Uuid, val title: String) : NanoIntent {
        override fun toString() = "Rename($conversationId, [redacted])"
    }

    data class Delete(val conversationId: Uuid, val stopReplyConfirmed: Boolean) : NanoIntent
    data class SelectModel(val configId: String) : NanoIntent
    data object RetrySave : NanoIntent
    data object FlushPending : NanoIntent
    data class ResetContext(val conversationId: Uuid) : NanoIntent
    data class LoadSummaries(val offset: Int) : NanoIntent
    data class LoadWindow(val conversationId: Uuid, val anchor: Long?) : NanoIntent
    data class UpdatePosition(
        val conversationId: Uuid,
        val position: ConversationPosition,
    ) : NanoIntent

    data object Back : NanoIntent
    data object OpenSettings : NanoIntent
    data object OpenSearch : NanoIntent
    data class PerformAction(val conversationId: Uuid, val action: AssistantAction) : NanoIntent {
        override fun toString() = "PerformAction($conversationId, [redacted])"
    }
}
