package com.skyd.anivu.ui.screen.feed.mute

import com.skyd.anivu.base.mvi.MviSingleEvent

sealed interface MuteFeedEvent : MviSingleEvent {
    sealed interface MuteResultEvent : MuteFeedEvent {
        data class Failed(val msg: String) : MuteResultEvent
    }
}