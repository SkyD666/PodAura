package com.skyd.anivu.ui.screen.settings.data.importexport.exportopml

import com.skyd.anivu.base.mvi.MviSingleEvent

sealed interface ExportOpmlEvent : MviSingleEvent {
    sealed interface ExportOpmlResultEvent : ExportOpmlEvent {
        data class Success(val time: Long) : ExportOpmlResultEvent
        data class Failed(val msg: String) : ExportOpmlResultEvent
    }
}