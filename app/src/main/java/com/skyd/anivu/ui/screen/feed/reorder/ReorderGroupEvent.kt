package com.skyd.anivu.ui.screen.feed.reorder

import com.skyd.anivu.ui.mvi.MviSingleEvent

sealed interface ReorderGroupEvent : MviSingleEvent {
    sealed interface GroupListResultEvent : ReorderGroupEvent {
        data class Failed(val msg: String) : GroupListResultEvent
    }

    sealed interface ReorderResultEvent : ReorderGroupEvent {
        data class Failed(val msg: String) : ReorderResultEvent
    }
}