package com.skyd.podaura.ext

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.io.files.Path

expect fun platformContext(): PlatformContext

suspend fun ImageLoader.getImage(
    context: PlatformContext = platformContext(),
    url: String,
): Path? = try {
    val request = ImageRequest.Builder(context)
        .data(url)
        .diskCachePolicy(CachePolicy.ENABLED)
        .build()
    when (val result = execute(request)) {
        is ErrorResult -> throw result.throwable
        is SuccessResult -> {
            diskCache!!.openSnapshot(url).use { snapshot ->
                return kotlinx.io.files.Path(snapshot!!.data.toString())
            }
        }
    }
} catch (e: Exception) {
    e.printStackTrace()
    null
}