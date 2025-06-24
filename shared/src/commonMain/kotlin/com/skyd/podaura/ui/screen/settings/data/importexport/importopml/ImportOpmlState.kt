package com.skyd.podaura.ui.screen.settings.data.importexport.importopml

import com.skyd.mvi.MviViewState

data class ImportOpmlState(
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = ImportOpmlState(
            loadingDialog = false,
        )
    }
}