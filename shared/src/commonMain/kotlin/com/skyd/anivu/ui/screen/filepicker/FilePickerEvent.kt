package com.skyd.anivu.ui.screen.filepicker

import com.skyd.anivu.ui.mvi.MviSingleEvent

sealed interface FilePickerEvent : MviSingleEvent {
    sealed interface FileListResultEvent : FilePickerEvent {
        data class Failed(val msg: String) : FileListResultEvent
    }
}