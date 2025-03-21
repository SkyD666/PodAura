package com.skyd.anivu.ui.screen.filepicker

import com.skyd.anivu.base.mvi.MviViewState
import com.skyd.anivu.config.Const
import java.io.File

data class FilePickerState(
    val path: String,
    val fileListState: FileListState,
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = FilePickerState(
            path = Const.INTERNAL_STORAGE,
            fileListState = FileListState.Init(),
            loadingDialog = false,
        )
    }
}

sealed class FileListState(open val loading: Boolean) {
    data class Success(val list: List<File>, override val loading: Boolean = false) :
        FileListState(loading)

    data class Init(override val loading: Boolean = false) : FileListState(loading)
    data class Failed(val msg: String, override val loading: Boolean = false) :
        FileListState(loading)
}