package com.skyd.podaura.util.coil.localmedia

import coil3.Image
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options

class LocalMediaFetcher(
    private val data: LocalMedia,
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        return getLocalMediaThumbnail(data.file.toString())?.let { image ->
            ImageFetchResult(
                image = image,
                isSampled = false,
                dataSource = DataSource.MEMORY,
            )
        }
    }

    class Factory : Fetcher.Factory<LocalMedia> {
        override fun create(data: LocalMedia, options: Options, imageLoader: ImageLoader): Fetcher {
            return LocalMediaFetcher(data)
        }
    }
}

expect fun getLocalMediaThumbnail(filePath: String): Image?