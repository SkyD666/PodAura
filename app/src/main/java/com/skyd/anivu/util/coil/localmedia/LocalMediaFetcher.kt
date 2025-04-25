package com.skyd.anivu.util.coil.localmedia

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.core.graphics.drawable.toDrawable
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options

class LocalMediaFetcher(
    private val data: LocalMedia,
    private val options: Options,
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        return getLocalMediaThumbnail(data.file.toString())?.let { bitmap ->
            ImageFetchResult(
                image = bitmap.toDrawable(options.context.resources).asImage(),
                isSampled = false,
                dataSource = DataSource.MEMORY,
            )
        }
    }

    class Factory : Fetcher.Factory<LocalMedia> {
        override fun create(data: LocalMedia, options: Options, imageLoader: ImageLoader): Fetcher {
            return LocalMediaFetcher(data, options)
        }
    }
}

private fun getLocalMediaThumbnail(filePath: String): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        with(retriever) {
            setDataSource(filePath)
            embeddedPicture?.let {
                BitmapFactory.decodeByteArray(it, 0, it.size)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } finally {
        retriever.release()
    }
}