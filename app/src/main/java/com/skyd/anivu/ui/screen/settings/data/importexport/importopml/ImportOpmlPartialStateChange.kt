package com.skyd.anivu.ui.screen.settings.data.importexport.importopml

import com.skyd.anivu.model.repository.importexport.IImportRepository.ImportOpmlResult


internal sealed interface ImportOpmlPartialStateChange {
    fun reduce(oldState: ImportOpmlState): ImportOpmlState

    sealed interface LoadingDialog : ImportOpmlPartialStateChange {
        data object Show : LoadingDialog {
            override fun reduce(oldState: ImportOpmlState) = oldState.copy(loadingDialog = true)
        }
    }

    data object Init : ImportOpmlPartialStateChange {
        override fun reduce(oldState: ImportOpmlState) = oldState.copy(loadingDialog = false)
    }

    sealed interface ImportOpml : ImportOpmlPartialStateChange {
        override fun reduce(oldState: ImportOpmlState): ImportOpmlState {
            return when (this) {
                is Success -> oldState.copy(
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    loadingDialog = false,
                )
            }
        }

        data class Success(val result: ImportOpmlResult) : ImportOpml
        data class Failed(val msg: String) : ImportOpml
    }
}