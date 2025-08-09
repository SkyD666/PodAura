package com.skyd.podaura.ui.screen.download

import com.skyd.mvi.MviViewState
import com.skyd.podaura.model.bean.download.DownloadInfoBean

data class DownloadState(
    val downloadListState: DownloadListState,
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = DownloadState(
            downloadListState = DownloadListState.Init,
            loadingDialog = false,
        )
    }
}

sealed interface DownloadListState {
    data class Success(val downloadInfoBeanList: List<DownloadInfoBean>) : DownloadListState
    data object Init : DownloadListState
    data object Loading : DownloadListState
    data class Failed(val msg: String) : DownloadListState
}