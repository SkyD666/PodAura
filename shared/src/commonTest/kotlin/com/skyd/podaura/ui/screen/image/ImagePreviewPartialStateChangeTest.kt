package com.skyd.podaura.ui.screen.image

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ImagePreviewPartialStateChangeTest {

    @Test
    fun initialStateIsLoadingWithWidgetsVisible() {
        val state = ImagePreviewState.initial()

        assertEquals(true, state.showWidgets)
        assertEquals(ImagePreviewLoadState.Loading, state.loadState)
        assertEquals(0, state.retryVersion)
        assertEquals(false, state.loadingDialog)
    }

    @Test
    fun toggleWidgetsChangesOnlyVisibility() {
        val initial = ImagePreviewState.initial()

        val hidden = ImagePreviewPartialStateChange.ToggleWidgets.reduce(initial)
        val visible = ImagePreviewPartialStateChange.ToggleWidgets.reduce(hidden)

        assertEquals(false, hidden.showWidgets)
        assertEquals(initial.loadState, hidden.loadState)
        assertEquals(initial.retryVersion, hidden.retryVersion)
        assertEquals(true, visible.showWidgets)
    }

    @Test
    fun loadCallbacksReplaceLoadState() {
        val initial = ImagePreviewState.initial()

        val success = ImagePreviewPartialStateChange.LoadSucceeded.reduce(initial)
        val failed = ImagePreviewPartialStateChange.LoadFailed("network error").reduce(success)
        val loading = ImagePreviewPartialStateChange.LoadStarted.reduce(failed)

        assertEquals(ImagePreviewLoadState.Success, success.loadState)
        assertEquals(
            "network error",
            assertIs<ImagePreviewLoadState.Failed>(failed.loadState).message,
        )
        assertEquals(ImagePreviewLoadState.Loading, loading.loadState)
    }

    @Test
    fun retryClearsFailureAndIncrementsVersion() {
        val failed = ImagePreviewState.initial().copy(
            loadState = ImagePreviewLoadState.Failed("network error"),
            retryVersion = 3,
        )

        val retrying = ImagePreviewPartialStateChange.Retry.reduce(failed)

        assertEquals(ImagePreviewLoadState.Loading, retrying.loadState)
        assertEquals(4, retrying.retryVersion)
        assertEquals(failed.showWidgets, retrying.showWidgets)
    }

    @Test
    fun imageOperationsToggleLoadingDialog() {
        val initial = ImagePreviewState.initial()

        val loading = ImagePreviewPartialStateChange.ImageOperationStarted.reduce(initial)
        val succeeded =
            ImagePreviewPartialStateChange.ImageOperationFinished.Success.reduce(loading)
        val failed = ImagePreviewPartialStateChange.ImageOperationFinished.Failed("error")
            .reduce(loading)

        assertEquals(true, loading.loadingDialog)
        assertEquals(false, succeeded.loadingDialog)
        assertEquals(false, failed.loadingDialog)
        assertEquals(initial.loadState, failed.loadState)
        assertEquals(initial.showWidgets, failed.showWidgets)
    }
}
