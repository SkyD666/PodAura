package com.skyd.podaura.ui.screen.feed.reorder.group

import com.skyd.mvi.MviSingleEvent

sealed interface ReorderGroupEvent : MviSingleEvent {
    sealed interface GroupListResultEvent : ReorderGroupEvent {
        data class Failed(val msg: String) : GroupListResultEvent
    }

    sealed interface ReorderResultEvent : ReorderGroupEvent {
        data class Failed(val msg: String) : ReorderResultEvent
    }
}