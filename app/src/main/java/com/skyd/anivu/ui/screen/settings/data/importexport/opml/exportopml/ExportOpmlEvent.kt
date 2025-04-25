package com.skyd.anivu.ui.screen.settings.data.importexport.opml.exportopml

import com.skyd.anivu.ui.mvi.MviSingleEvent

sealed interface ExportOpmlEvent : MviSingleEvent {
    sealed interface ExportOpmlResultEvent : ExportOpmlEvent {
        data class Success(val time: Long) : ExportOpmlResultEvent
        data class Failed(val msg: String) : ExportOpmlResultEvent
    }
}