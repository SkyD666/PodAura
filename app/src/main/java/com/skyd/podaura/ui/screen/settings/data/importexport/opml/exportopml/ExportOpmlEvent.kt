package com.skyd.podaura.ui.screen.settings.data.importexport.opml.exportopml

import com.skyd.podaura.ui.mvi.MviSingleEvent

sealed interface ExportOpmlEvent : MviSingleEvent {
    sealed interface ExportOpmlResultEvent : ExportOpmlEvent {
        data class Success(val time: Long) : ExportOpmlResultEvent
        data class Failed(val msg: String) : ExportOpmlResultEvent
    }
}