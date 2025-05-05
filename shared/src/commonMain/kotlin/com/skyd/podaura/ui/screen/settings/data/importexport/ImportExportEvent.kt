package com.skyd.podaura.ui.screen.settings.data.importexport

import com.skyd.podaura.ui.mvi.MviSingleEvent

sealed interface ImportExportEvent : MviSingleEvent {
    sealed interface ExportOpmlResultEvent : ImportExportEvent {
        data class Success(val time: Long) : ExportOpmlResultEvent
        data class Failed(val msg: String) : ExportOpmlResultEvent
    }

    sealed interface ImportResultEvent : ImportExportEvent {
        data class Success(val time: Long) : ImportResultEvent
        data class Failed(val msg: String) : ImportResultEvent
    }

    sealed interface ExportResultEvent : ImportExportEvent {
        data class Success(val time: Long) : ExportResultEvent
        data class Failed(val msg: String) : ExportResultEvent
    }
}