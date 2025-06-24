package com.skyd.podaura.ui.screen.playlist

import com.skyd.mvi.MviSingleEvent

sealed interface PlaylistEvent : MviSingleEvent {
    sealed interface CreateResultEvent : PlaylistEvent {
        data class Failed(val msg: String) : CreateResultEvent
    }

    sealed interface RenameResultEvent : PlaylistEvent {
        data class Failed(val msg: String) : RenameResultEvent
    }

    sealed interface ReorderResultEvent : PlaylistEvent {
        data class Failed(val msg: String) : ReorderResultEvent
    }

    sealed interface DeleteResultEvent : PlaylistEvent {
        data class Failed(val msg: String) : DeleteResultEvent
    }
}