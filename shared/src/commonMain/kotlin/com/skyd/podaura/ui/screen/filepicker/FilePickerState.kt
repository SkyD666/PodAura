package com.skyd.podaura.ui.screen.filepicker

import com.skyd.mvi.MviViewState
import com.skyd.fundation.config.Const
import com.skyd.fundation.config.DEFAULT_FILE_PICKER_PATH
import kotlinx.io.files.Path

data class FilePickerState(
    val path: String,
    val fileListState: FileListState,
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = FilePickerState(
            path = Const.DEFAULT_FILE_PICKER_PATH,
            fileListState = FileListState.Init(),
            loadingDialog = false,
        )
    }
}

sealed class FileListState(open val loading: Boolean) {
    data class Success(val list: List<Path>, override val loading: Boolean = false) :
        FileListState(loading)

    data class Init(override val loading: Boolean = false) : FileListState(loading)
    data class Failed(val msg: String, override val loading: Boolean = false) :
        FileListState(loading)
}