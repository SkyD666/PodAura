package com.skyd.anivu.ui.screen.settings.data.importexport.opml.importopml

import com.skyd.anivu.ui.mvi.MviViewState

data class ImportOpmlState(
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = ImportOpmlState(
            loadingDialog = false,
        )
    }
}