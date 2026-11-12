package com.skyd.podaura.podnya.ui.screen.chat

import com.skyd.mvi.MviViewState
import com.skyd.podaura.podnya.model.bean.AssistantConversationsState
import com.skyd.podaura.podnya.settings.AssistantSettings

data class PodnyaState(
    val conversations: AssistantConversationsState,
    val settings: AssistantSettings? = null,
    val available: Boolean,
    val settingsFailed: Boolean = false,
) : MviViewState {
    val session get() = conversations.selected
    val running get() = session.id in conversations.runningIds
    val modelAvailable get() = session.modelConfigId == null || session.modelConfigId == settings?.configId
    val ready: Boolean get() = available && modelAvailable && settings?.let { it.configured && it.consentAccepted } == true

    companion object {
        fun initial(conversations: AssistantConversationsState, available: Boolean) =
            PodnyaState(conversations = conversations, available = available)
    }
}
