package com.skyd.podaura.nano.ui.screen.chat

import com.skyd.podaura.nano.model.bean.conversation.AssistantConversationsState
import com.skyd.podaura.nano.settings.AssistantProfiles

internal sealed interface NanoPartialStateChange {
    fun reduce(oldState: NanoState): NanoState

    // The repository publishes session snapshots separately; command completion changes no state.
    data object CommandCompleted : NanoPartialStateChange {
        override fun reduce(oldState: NanoState) = oldState
    }

    data class Notify(val event: NanoEvent) : NanoPartialStateChange {
        override fun reduce(oldState: NanoState) = oldState
    }

    data object SettingsUnavailable : NanoPartialStateChange {
        override fun reduce(oldState: NanoState) =
            oldState.copy(profiles = null, settingsFailed = true)
    }

    data class Session(val value: AssistantConversationsState) : NanoPartialStateChange {
        override fun reduce(oldState: NanoState) = oldState.copy(conversations = value)
    }

    data class Settings(val value: AssistantProfiles) : NanoPartialStateChange {
        override fun reduce(oldState: NanoState) =
            oldState.copy(profiles = value, settingsFailed = false)
    }
}
