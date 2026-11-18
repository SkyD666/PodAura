package com.skyd.podaura.podnya.ui.screen.chat

import com.skyd.podaura.podnya.model.bean.conversation.AssistantConversationsState
import com.skyd.podaura.podnya.settings.AssistantProfiles

internal sealed interface PodnyaPartialStateChange {
    fun reduce(oldState: PodnyaState): PodnyaState

    // The repository publishes session snapshots separately; command completion changes no state.
    data object CommandCompleted : PodnyaPartialStateChange {
        override fun reduce(oldState: PodnyaState) = oldState
    }

    data class Notify(val event: PodnyaEvent) : PodnyaPartialStateChange {
        override fun reduce(oldState: PodnyaState) = oldState
    }

    data object SettingsUnavailable : PodnyaPartialStateChange {
        override fun reduce(oldState: PodnyaState) =
            oldState.copy(profiles = null, settingsFailed = true)
    }

    data class Session(val value: AssistantConversationsState) : PodnyaPartialStateChange {
        override fun reduce(oldState: PodnyaState) = oldState.copy(conversations = value)
    }

    data class Settings(val value: AssistantProfiles) : PodnyaPartialStateChange {
        override fun reduce(oldState: PodnyaState) =
            oldState.copy(profiles = value, settingsFailed = false)
    }
}
