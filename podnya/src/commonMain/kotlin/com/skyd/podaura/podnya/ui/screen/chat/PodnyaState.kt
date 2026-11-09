package com.skyd.podaura.podnya.ui.screen.chat

import com.skyd.mvi.MviViewState
import com.skyd.podaura.podnya.model.bean.AssistantSessionState
import com.skyd.podaura.podnya.settings.AssistantSettings

data class PodnyaState(
    val session: AssistantSessionState,
    val settings: AssistantSettings? = null,
    val available: Boolean,
    val settingsFailed: Boolean = false,
) : MviViewState {
    val ready: Boolean get() = available && settings?.let { it.configured && it.consentAccepted } == true

    companion object {
        fun initial(session: AssistantSessionState, available: Boolean) =
            PodnyaState(session = session, available = available)
    }
}
