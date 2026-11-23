package com.skyd.podaura.nano.ui.screen.chat

import com.skyd.mvi.MviViewState
import com.skyd.podaura.nano.core.UnreadableContext
import com.skyd.podaura.nano.model.bean.conversation.AssistantConversationsState
import com.skyd.podaura.nano.model.bean.conversation.ConversationStorageState
import com.skyd.podaura.nano.settings.AssistantProfiles

data class NanoState(
    val conversations: AssistantConversationsState,
    val profiles: AssistantProfiles? = null,
    val available: Boolean,
    val settingsFailed: Boolean = false,
) : MviViewState {
    val session get() = conversations.selected
    val running get() = session.id in conversations.runningIds
    val ready: Boolean
        get() = conversations.storage == ConversationStorageState.READY &&
                session.context !is UnreadableContext &&
                available &&
                profiles?.active?.configured == true

    companion object {
        fun initial(conversations: AssistantConversationsState, available: Boolean) =
            NanoState(conversations = conversations, available = available)
    }
}
