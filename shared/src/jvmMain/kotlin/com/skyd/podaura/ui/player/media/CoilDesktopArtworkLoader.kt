package com.skyd.podaura.ui.player.media

import coil3.PlatformContext
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Precision
import coil3.size.Scale
import coil3.toBitmap
import coil3.util.DebugLogger
import com.skyd.podaura.ui.component.imageLoaderBuilder
import com.skyd.podaura.util.coil.localmedia.LocalMedia
import com.skyd.podaura.util.coil.localmedia.LocalMediaImageLogger
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

internal object CoilDesktopArtworkLoader : DesktopArtworkLoader {
    private const val MAX_ARTWORK_SIZE = 1024
    private val context = PlatformContext.INSTANCE
    private val imageLoader by lazy {
        context.imageLoaderBuilder()
            .logger(LocalMediaImageLogger(DebugLogger()))
            .build()
    }

    override suspend fun load(source: Any): DesktopArtworkData? {
        val request = ImageRequest.Builder(context)
            .data(source)
            .size(MAX_ARTWORK_SIZE, MAX_ARTWORK_SIZE)
            .scale(Scale.FIT)
            .precision(Precision.INEXACT)
            .apply {
                if (source is LocalMedia) {
                    interceptorCoroutineContext(Dispatchers.IO)
                }
            }
            .build()
        return when (val result = imageLoader.execute(request)) {
            is ErrorResult -> null
            is SuccessResult -> result.image.toBitmap().let { bitmap ->
                Image.makeFromBitmap(bitmap).use { image ->
                    image.encodeToData(EncodedImageFormat.PNG, 100)?.use { data ->
                        DesktopArtworkData(
                            pngBytes = data.bytes,
                            width = image.width,
                            height = image.height,
                        )
                    }
                }
            }
        }
    }
}
