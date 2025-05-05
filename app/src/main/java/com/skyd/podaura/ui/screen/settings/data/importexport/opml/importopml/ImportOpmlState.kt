package com.skyd.podaura.ui.screen.settings.data.importexport.opml.importopml

import com.skyd.podaura.ui.mvi.MviViewState

data class ImportOpmlState(
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = ImportOpmlState(
            loadingDialog = false,
        )
    }
}