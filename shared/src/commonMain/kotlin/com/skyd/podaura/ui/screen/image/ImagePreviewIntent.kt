package com.skyd.podaura.ui.screen.image

import androidx.compose.ui.platform.Clipboard
import com.skyd.mvi.MviIntent

sealed interface ImagePreviewIntent : MviIntent {
    data object ToggleWidgets : ImagePreviewIntent
    data object LoadStarted : ImagePreviewIntent
    data object LoadSucceeded : ImagePreviewIntent
    data class LoadFailed(val message: String) : ImagePreviewIntent
    data object Retry : ImagePreviewIntent
    data class DownloadImage(val image: String, val title: String?) : ImagePreviewIntent
    data class ShareImage(val image: String) : ImagePreviewIntent
    data class CopyImage(val image: String, val clipboard: Clipboard) : ImagePreviewIntent
}
