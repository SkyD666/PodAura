package com.skyd.podaura.ui.screen.image

import com.skyd.mvi.AbstractMviViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan

class ImagePreviewViewModel :
    AbstractMviViewModel<ImagePreviewIntent, ImagePreviewState, ImagePreviewEvent>() {

    override val viewState: StateFlow<ImagePreviewState>

    init {
        val initialVS = ImagePreviewState.initial()

        viewState = intentFlow
            .toImagePreviewPartialStateChangeFlow()
            .debugLog("ImagePreviewPartialStateChange")
            .scan(initialVS) { state, change -> change.reduce(state) }
            .debugLog("ViewState")
            .toState(initialVS)
    }

    private fun Flow<ImagePreviewIntent>.toImagePreviewPartialStateChangeFlow():
            Flow<ImagePreviewPartialStateChange> =
        map { intent ->
            when (intent) {
                ImagePreviewIntent.ToggleWidgets ->
                    ImagePreviewPartialStateChange.ToggleWidgets

                ImagePreviewIntent.LoadStarted ->
                    ImagePreviewPartialStateChange.LoadStarted

                ImagePreviewIntent.LoadSucceeded ->
                    ImagePreviewPartialStateChange.LoadSucceeded

                is ImagePreviewIntent.LoadFailed ->
                    ImagePreviewPartialStateChange.LoadFailed(intent.message)

                ImagePreviewIntent.Retry ->
                    ImagePreviewPartialStateChange.Retry
            }
        }
}
