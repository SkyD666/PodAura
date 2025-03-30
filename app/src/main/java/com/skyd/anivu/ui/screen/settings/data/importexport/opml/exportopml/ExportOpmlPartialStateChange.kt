package com.skyd.anivu.ui.screen.settings.data.importexport.opml.exportopml


internal sealed interface ExportOpmlPartialStateChange {
    fun reduce(oldState: ExportOpmlState): ExportOpmlState

    sealed interface LoadingDialog : ExportOpmlPartialStateChange {
        data object Show : LoadingDialog {
            override fun reduce(oldState: ExportOpmlState) = oldState.copy(loadingDialog = true)
        }
    }

    data object Init : ExportOpmlPartialStateChange {
        override fun reduce(oldState: ExportOpmlState) = oldState.copy(loadingDialog = false)
    }

    sealed interface ExportOpml : ExportOpmlPartialStateChange {
        override fun reduce(oldState: ExportOpmlState): ExportOpmlState {
            return when (this) {
                is Success -> oldState.copy(
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data class Success(val time: Long) : ExportOpml
        data class Failed(val msg: String) : ExportOpml
    }
}