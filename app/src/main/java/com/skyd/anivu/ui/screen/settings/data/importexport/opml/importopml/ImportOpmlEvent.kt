package com.skyd.anivu.ui.screen.settings.data.importexport.opml.importopml

import com.skyd.anivu.ui.mvi.MviSingleEvent
import com.skyd.anivu.model.repository.importexport.opml.IImportOpmlRepository.ImportOpmlResult

sealed interface ImportOpmlEvent : MviSingleEvent {
    sealed interface ImportOpmlResultEvent : ImportOpmlEvent {
        data class Success(val result: ImportOpmlResult) : ImportOpmlResultEvent
        data class Failed(val msg: String) : ImportOpmlResultEvent
    }
}