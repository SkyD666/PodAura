package com.skyd.podaura.ui.screen.image

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import coil3.request.ImageRequest
import com.github.panpf.zoomimage.CoilZoomAsyncImage

@Composable
internal actual fun ZoomableAsyncImage(
    model: ImageRequest,
    contentDescription: String?,
    imageLoader: ImageLoader,
    modifier: Modifier,
    onLoading: () -> Unit,
    onSuccess: () -> Unit,
    onError: (Throwable) -> Unit,
    onTap: () -> Unit,
) {
    CoilZoomAsyncImage(
        model = model,
        contentDescription = contentDescription,
        imageLoader = imageLoader,
        modifier = modifier,
        onLoading = { onLoading() },
        onSuccess = { onSuccess() },
        onError = { onError(it.result.throwable) },
        onTap = { onTap() },
    )
}
