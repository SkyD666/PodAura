package com.skyd.podaura.ui.screen.settings.data.importexport.opml.exportopml

import com.skyd.podaura.ui.mvi.MviViewState

data class ExportOpmlState(
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = ExportOpmlState(
            loadingDialog = false,
        )
    }
}