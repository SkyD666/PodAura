package com.skyd.anivu.ext

import android.content.Context
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import java.io.File

suspend fun ImageLoader.getImage(context: Context, url: String): File? = try {
    val request = ImageRequest.Builder(context)
        .data(url)
        .diskCachePolicy(CachePolicy.ENABLED)
        .build()
    when (execute(request)) {
        is ErrorResult -> null
        is SuccessResult -> {
            diskCache!!.openSnapshot(url).use { snapshot ->
                snapshot!!.data.toFile()
            }
        }
    }
} catch (e: Exception) {
    e.printStackTrace()
    null
}