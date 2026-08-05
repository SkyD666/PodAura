package com.skyd.podaura.ui.screen.image

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest

private const val MinImageScale = 1f
private const val MaxImageScale = 5f

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
    onLongPress: () -> Unit,
) {
    var scale by remember(model) { mutableFloatStateOf(MinImageScale) }
    var offset by remember(model) { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    fun constrainedOffset(candidate: Offset, imageScale: Float): Offset {
        val maxX = viewportSize.width * (imageScale - MinImageScale) / 2f
        val maxY = viewportSize.height * (imageScale - MinImageScale) / 2f
        return Offset(
            x = candidate.x.coerceIn(-maxX, maxX),
            y = candidate.y.coerceIn(-maxY, maxY),
        )
    }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(MinImageScale, MaxImageScale)
        offset = if (newScale == MinImageScale) {
            Offset.Zero
        } else {
            constrainedOffset(offset + panChange, newScale)
        }
        scale = newScale
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged {
                viewportSize = it
                offset = constrainedOffset(offset, scale)
            }
            .transformable(transformState)
            .combinedClickable(onClick = onTap, onLongClick = onLongPress),
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            imageLoader = imageLoader,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            contentScale = ContentScale.Fit,
            onLoading = { onLoading() },
            onSuccess = { onSuccess() },
            onError = { onError(it.result.throwable) },
        )
    }
}
