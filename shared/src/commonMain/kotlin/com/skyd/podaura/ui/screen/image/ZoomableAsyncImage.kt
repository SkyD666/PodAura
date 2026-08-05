package com.skyd.podaura.ui.screen.image

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import coil3.request.ImageRequest

@Composable
internal expect fun ZoomableAsyncImage(
    model: ImageRequest,
    contentDescription: String?,
    imageLoader: ImageLoader,
    modifier: Modifier,
    onLoading: () -> Unit,
    onSuccess: () -> Unit,
    onError: (Throwable) -> Unit,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
)
