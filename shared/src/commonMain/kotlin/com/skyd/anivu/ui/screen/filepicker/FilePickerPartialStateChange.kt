package com.skyd.anivu.ui.screen.filepicker

import kotlinx.io.files.Path


internal sealed interface FilePickerPartialStateChange {
    fun reduce(oldState: FilePickerState): FilePickerState

    sealed interface LoadingDialog : FilePickerPartialStateChange {
        data object Show : LoadingDialog {
            override fun reduce(oldState: FilePickerState) = oldState.copy(loadingDialog = true)
        }
    }

    sealed interface FileListResult : FilePickerPartialStateChange {
        override fun reduce(oldState: FilePickerState): FilePickerState {
            return when (this) {
                is Success -> oldState.copy(
                    path = path,
                    fileListState = FileListState.Success(list = list),
                    loadingDialog = false,
                )

                is Failed -> oldState.copy(
                    path = path,
                    fileListState = FileListState.Failed(msg = msg),
                    loadingDialog = false,
                )

                Loading -> oldState.copy(
                    fileListState = oldState.fileListState.let {
                        when (it) {
                            is FileListState.Failed -> it.copy(loading = true)
                            is FileListState.Init -> it.copy(loading = true)
                            is FileListState.Success -> it.copy(loading = true)
                        }
                    },
                    loadingDialog = false,
                )
            }
        }

        data class Success(val path: String, val list: List<Path>) : FileListResult
        data class Failed(val path: String, val msg: String) : FileListResult
        data object Loading : FileListResult
    }
}
