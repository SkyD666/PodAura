package com.skyd.podaura.ui.screen.feed.reorder

import com.skyd.podaura.ui.mvi.MviSingleEvent

sealed interface ReorderGroupEvent : MviSingleEvent {
    sealed interface GroupListResultEvent : ReorderGroupEvent {
        data class Failed(val msg: String) : GroupListResultEvent
    }

    sealed interface ReorderResultEvent : ReorderGroupEvent {
        data class Failed(val msg: String) : ReorderResultEvent
    }
}