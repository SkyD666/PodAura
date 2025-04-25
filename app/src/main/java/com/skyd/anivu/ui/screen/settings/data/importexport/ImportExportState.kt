package com.skyd.anivu.ui.screen.settings.data.importexport

import com.skyd.anivu.ui.mvi.MviViewState

data class ImportExportState(
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = ImportExportState(
            loadingDialog = false,
        )
    }
}