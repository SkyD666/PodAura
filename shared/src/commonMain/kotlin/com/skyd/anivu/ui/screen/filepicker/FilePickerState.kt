package com.skyd.anivu.ui.screen.filepicker

import com.skyd.anivu.config.Const
import com.skyd.anivu.config.DEFAULT_FILE_PICKER_PATH
import com.skyd.anivu.ui.mvi.MviViewState
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