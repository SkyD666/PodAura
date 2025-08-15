package com.skyd.podaura.ui.screen.feed.reorder.feed

import com.skyd.mvi.MviSingleEvent

sealed interface ReorderFeedEvent : MviSingleEvent {
    sealed interface FeedListResultEvent : ReorderFeedEvent {
        data class Failed(val msg: String) : FeedListResultEvent
    }

    sealed interface ReorderResultEvent : ReorderFeedEvent {
        data class Failed(val msg: String) : ReorderResultEvent
    }
}