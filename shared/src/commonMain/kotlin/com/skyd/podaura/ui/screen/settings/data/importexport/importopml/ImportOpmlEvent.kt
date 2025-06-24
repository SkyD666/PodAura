package com.skyd.podaura.ui.screen.settings.data.importexport.importopml

import com.skyd.mvi.MviSingleEvent
import com.skyd.podaura.model.repository.importexport.opml.IImportOpmlRepository.ImportOpmlResult

sealed interface ImportOpmlEvent : MviSingleEvent {
    sealed interface ImportOpmlResultEvent : ImportOpmlEvent {
        data class Success(val result: ImportOpmlResult) : ImportOpmlResultEvent
        data class Failed(val msg: String) : ImportOpmlResultEvent
    }
}