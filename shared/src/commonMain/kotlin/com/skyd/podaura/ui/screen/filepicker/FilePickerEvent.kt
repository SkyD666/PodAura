package com.skyd.podaura.ui.screen.filepicker

import com.skyd.mvi.MviSingleEvent

sealed interface FilePickerEvent : MviSingleEvent {
    sealed interface FileListResultEvent : FilePickerEvent {
        data class Failed(val msg: String) : FileListResultEvent
    }
}