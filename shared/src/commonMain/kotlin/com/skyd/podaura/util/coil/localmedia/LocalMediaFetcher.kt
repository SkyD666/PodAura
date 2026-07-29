package com.skyd.podaura.util.coil.localmedia

import coil3.ComponentRegistry
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.key.Keyer
import coil3.request.Options
import coil3.util.Logger
import okio.Buffer

class LocalMediaFetcher(
    private val data: LocalMedia,
    private val options: Options,
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val thumbnailData = getLocalMediaThumbnailData(data.file)
            ?: throw LocalMediaArtworkNotFoundException(data)
        return SourceFetchResult(
            source = ImageSource(
                source = Buffer().apply { write(thumbnailData) },
                fileSystem = options.fileSystem,
            ),
            mimeType = null,
            dataSource = DataSource.DISK,
        )
    }

    class Factory : Fetcher.Factory<LocalMedia> {
        override fun create(data: LocalMedia, options: Options, imageLoader: ImageLoader): Fetcher {
            return LocalMediaFetcher(data, options)
        }
    }
}

class LocalMediaKeyer : Keyer<LocalMedia> {
    override fun key(data: LocalMedia, options: Options): String {
        val revision = getLocalMediaFileRevision(data.file)
        return buildString {
            append("local-media-thumbnail:")
            append(data.file)
            if (revision != null) {
                append(':')
                append(revision)
            }
        }
    }
}

class LocalMediaArtworkNotFoundException(
    val localMedia: LocalMedia,
) : Exception("No embedded artwork found in: ${localMedia.file}")

internal class LocalMediaImageLogger(
    private val delegate: Logger,
) : Logger {
    override var minLevel: Logger.Level
        get() = delegate.minLevel
        set(value) {
            delegate.minLevel = value
        }

    override fun log(
        tag: String,
        level: Logger.Level,
        message: String?,
        throwable: Throwable?,
    ) {
        if (throwable is LocalMediaArtworkNotFoundException) return
        delegate.log(tag, level, message, throwable)
    }
}

fun ComponentRegistry.Builder.addLocalMediaComponents() {
    add(LocalMediaKeyer())
    add(LocalMediaFetcher.Factory())
}

expect fun getLocalMediaThumbnailData(filePath: String): ByteArray?

/**
 * Returns a cheap representation of the underlying file revision, or null when [filePath] cannot
 * be resolved to a regular file. This performs filesystem I/O and LocalMedia image requests must
 * run their interceptor coroutine context on an I/O dispatcher.
 */
expect fun getLocalMediaFileRevision(filePath: String): String?
