package com.skyd.podaura.podnya.ui.screen.chat

import com.skyd.podaura.podnya.model.bean.AssistantConversationsState
import com.skyd.podaura.podnya.settings.AssistantSettings

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
            oldState.copy(settings = null, settingsFailed = true)
    }

    data class Session(val value: AssistantConversationsState) : PodnyaPartialStateChange {
        override fun reduce(oldState: PodnyaState) = oldState.copy(conversations = value)
    }

    data class Settings(val value: AssistantSettings) : PodnyaPartialStateChange {
        override fun reduce(oldState: PodnyaState) =
            oldState.copy(settings = value, settingsFailed = false)
    }
}
