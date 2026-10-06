package com.skyd.podaura.ui.player.media

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.SuccessResult
import coil3.size.Precision
import coil3.size.Scale
import coil3.toBitmap
import com.skyd.podaura.ui.component.imageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.impl.use

/** Share the player's Coil fetchers, decoders and cache for native system artwork. */
internal suspend fun loadAppleArtwork(source: Any, imageLoader: ImageLoader): DesktopArtworkData? =
    withContext(Dispatchers.IO) {
        val request = imageRequest(source, PlatformContext.INSTANCE).newBuilder()
            .size(1024, 1024)
            .scale(Scale.FIT)
            .precision(Precision.INEXACT)
            .build()
        val result = imageLoader.execute(request) as? SuccessResult ?: return@withContext null
        Image.makeFromBitmap(result.image.toBitmap()).use { image ->
            image.encodeToData(EncodedImageFormat.PNG, 100)?.use { data ->
                DesktopArtworkData(data.bytes, image.width, image.height)
            }
        }
    }
