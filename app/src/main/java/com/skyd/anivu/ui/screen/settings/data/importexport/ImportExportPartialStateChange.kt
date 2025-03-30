package com.skyd.anivu.ui.screen.settings.data.importexport


internal sealed interface ImportExportPartialStateChange {
    fun reduce(oldState: ImportExportState): ImportExportState

    sealed interface LoadingDialog : ImportExportPartialStateChange {
        data object Show : LoadingDialog {
            override fun reduce(oldState: ImportExportState) = oldState.copy(loadingDialog = true)
        }
    }

    data object Init : ImportExportPartialStateChange {
        override fun reduce(oldState: ImportExportState) = oldState.copy(loadingDialog = false)
    }

    sealed interface ImportPrefer : ImportExportPartialStateChange {
        override fun reduce(oldState: ImportExportState): ImportExportState {
            return when (this) {
                is Success -> oldState.copy(
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data class Success(val time: Long) : ImportPrefer
        data class Failed(val msg: String) : ImportPrefer
    }

    sealed interface ExportPrefer : ImportExportPartialStateChange {
        override fun reduce(oldState: ImportExportState): ImportExportState {
            return when (this) {
                is Success -> oldState.copy(
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data class Success(val time: Long) : ExportPrefer
        data class Failed(val msg: String) : ExportPrefer
    }
}