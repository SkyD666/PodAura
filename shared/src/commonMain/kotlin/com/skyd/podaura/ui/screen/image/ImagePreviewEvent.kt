package com.skyd.podaura.ui.screen.image

import com.skyd.mvi.MviSingleEvent

sealed interface ImagePreviewEvent : MviSingleEvent {
    data class ImageOperationFailed(val message: String) : ImagePreviewEvent
}
