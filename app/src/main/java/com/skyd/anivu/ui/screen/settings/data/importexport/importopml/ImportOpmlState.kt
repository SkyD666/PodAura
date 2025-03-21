package com.skyd.anivu.ui.screen.settings.data.importexport.importopml

import com.skyd.anivu.base.mvi.MviViewState

data class ImportOpmlState(
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = ImportOpmlState(
            loadingDialog = false,
        )
    }
}