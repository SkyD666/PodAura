package com.skyd.podaura.ui.screen.image

import com.skyd.mvi.AbstractMviViewModel
import com.skyd.podaura.ext.catchMap
import com.skyd.podaura.ext.startWith
import com.skyd.podaura.model.repository.ImageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan

class ImagePreviewViewModel(
    private val imageRepository: ImageRepository,
) :
    AbstractMviViewModel<ImagePreviewIntent, ImagePreviewState, ImagePreviewEvent>() {

    override val viewState: StateFlow<ImagePreviewState>

    init {
        val initialVS = ImagePreviewState.initial()

        viewState = intentFlow
            .toImagePreviewPartialStateChangeFlow()
            .debugLog("ImagePreviewPartialStateChange")
            .sendSingleEvent()
            .scan(initialVS) { state, change -> change.reduce(state) }
            .debugLog("ViewState")
            .toState(initialVS)
    }

    private fun Flow<ImagePreviewPartialStateChange>.sendSingleEvent() = onEach { change ->
        if (change is ImagePreviewPartialStateChange.ImageOperationFinished.Failed) {
            sendEvent(ImagePreviewEvent.ImageOperationFailed(change.message))
        }
    }

    private fun Flow<ImagePreviewIntent>.toImagePreviewPartialStateChangeFlow():
            Flow<ImagePreviewPartialStateChange> = merge(
        filter { intent ->
            intent !is ImagePreviewIntent.DownloadImage &&
                    intent !is ImagePreviewIntent.ShareImage &&
                    intent !is ImagePreviewIntent.CopyImage
        }.map { intent ->
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

                else -> error("Image operation intent must be handled asynchronously")
            }
        },
        filterIsInstance<ImagePreviewIntent.DownloadImage>().flatMapConcat { intent ->
            imageRepository.downloadImage(intent.image, intent.title).map {
                ImagePreviewPartialStateChange.ImageOperationFinished.Success
            }.startWith(ImagePreviewPartialStateChange.ImageOperationStarted).catchMap {
                ImagePreviewPartialStateChange.ImageOperationFinished.Failed(it.message.toString())
            }
        },
        filterIsInstance<ImagePreviewIntent.ShareImage>().flatMapConcat { intent ->
            imageRepository.shareImage(intent.image).map {
                ImagePreviewPartialStateChange.ImageOperationFinished.Success
            }.startWith(ImagePreviewPartialStateChange.ImageOperationStarted).catchMap {
                ImagePreviewPartialStateChange.ImageOperationFinished.Failed(it.message.toString())
            }
        },
        filterIsInstance<ImagePreviewIntent.CopyImage>().flatMapConcat { intent ->
            imageRepository.copyImage(intent.image, intent.clipboard).map {
                ImagePreviewPartialStateChange.ImageOperationFinished.Success
            }.startWith(ImagePreviewPartialStateChange.ImageOperationStarted).catchMap {
                ImagePreviewPartialStateChange.ImageOperationFinished.Failed(it.message.toString())
            }
        },
    )
}
