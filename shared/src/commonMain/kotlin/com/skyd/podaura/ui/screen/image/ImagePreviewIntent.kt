package com.skyd.podaura.ui.screen.image

import com.skyd.mvi.MviIntent

sealed interface ImagePreviewIntent : MviIntent {
    data object ToggleWidgets : ImagePreviewIntent
    data object LoadStarted : ImagePreviewIntent
    data object LoadSucceeded : ImagePreviewIntent
    data class LoadFailed(val message: String) : ImagePreviewIntent
    data object Retry : ImagePreviewIntent
}
