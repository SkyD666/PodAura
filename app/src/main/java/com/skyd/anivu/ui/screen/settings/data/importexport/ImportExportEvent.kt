package com.skyd.anivu.ui.screen.settings.data.importexport

import com.skyd.anivu.ui.mvi.MviSingleEvent

sealed interface ImportExportEvent : MviSingleEvent {
    sealed interface ImportResultEvent : ImportExportEvent {
        data class Success(val time: Long) : ImportResultEvent
        data class Failed(val msg: String) : ImportResultEvent
    }

    sealed interface ExportResultEvent : ImportExportEvent {
        data class Success(val time: Long) : ExportResultEvent
        data class Failed(val msg: String) : ExportResultEvent
    }
}