package com.skyd.podaura.ui.screen.settings.data.importexport

import com.skyd.mvi.MviViewState

data class ImportExportState(
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = ImportExportState(
            loadingDialog = false,
        )
    }
}