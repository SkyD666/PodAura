package com.skyd.podaura.ui.screen.image

internal sealed interface ImagePreviewPartialStateChange {
    fun reduce(oldState: ImagePreviewState): ImagePreviewState

    data object ToggleWidgets : ImagePreviewPartialStateChange {
        override fun reduce(oldState: ImagePreviewState) = oldState.copy(
            showWidgets = !oldState.showWidgets,
        )
    }

    data object LoadStarted : ImagePreviewPartialStateChange {
        override fun reduce(oldState: ImagePreviewState) = oldState.copy(
            loadState = ImagePreviewLoadState.Loading,
        )
    }

    data object LoadSucceeded : ImagePreviewPartialStateChange {
        override fun reduce(oldState: ImagePreviewState) = oldState.copy(
            loadState = ImagePreviewLoadState.Success,
        )
    }

    data class LoadFailed(val message: String) : ImagePreviewPartialStateChange {
        override fun reduce(oldState: ImagePreviewState) = oldState.copy(
            loadState = ImagePreviewLoadState.Failed(message),
        )
    }

    data object Retry : ImagePreviewPartialStateChange {
        override fun reduce(oldState: ImagePreviewState) = oldState.copy(
            loadState = ImagePreviewLoadState.Loading,
            retryVersion = oldState.retryVersion + 1,
        )
    }
}
