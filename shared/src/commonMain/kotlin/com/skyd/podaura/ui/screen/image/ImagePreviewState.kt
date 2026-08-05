package com.skyd.podaura.ui.screen.image

import com.skyd.mvi.MviViewState

data class ImagePreviewState(
    val showWidgets: Boolean,
    val loadState: ImagePreviewLoadState,
    val retryVersion: Int,
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = ImagePreviewState(
            showWidgets = true,
            loadState = ImagePreviewLoadState.Loading,
            retryVersion = 0,
            loadingDialog = false,
        )
    }
}

sealed interface ImagePreviewLoadState {
    data object Loading : ImagePreviewLoadState
    data object Success : ImagePreviewLoadState
    data class Failed(val message: String) : ImagePreviewLoadState
}
