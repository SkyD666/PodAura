package com.skyd.podaura.util.coil.localmedia

import coil3.ComponentRegistry
import coil3.ImageLoader
import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.disk.DiskCache
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.key.Keyer
import coil3.request.Options
import coil3.util.Logger
import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.platform
import com.skyd.podaura.media.MediaTypes
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okio.Buffer

class LocalMediaFetcher(
    private val data: LocalMedia,
    private val options: Options,
    private val imageLoader: ImageLoader,
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val cache = imageLoader.diskCache
        val key = LocalMediaKeyer().key(data, options) + ":frame-10%-320-v1"
        if (options.diskCachePolicy.readEnabled) {
            cache?.openSnapshot(key)?.let { return cachedResult(cache, key, it) }
        }
        val thumbnailData = getLocalMediaThumbnailData(data.file)
            ?: throw LocalMediaArtworkNotFoundException(data)
        currentCoroutineContext().ensureActive()
        if (cache != null && options.diskCachePolicy.writeEnabled) {
            cache.openEditor(key)?.let { editor ->
                try {
                    cache.fileSystem.write(editor.metadata) {}
                    cache.fileSystem.write(editor.data) { write(thumbnailData) }
                    editor.commitAndOpenSnapshot()?.let {
                        return cachedResult(cache, key, it)
                    }
                } catch (_: Exception) {
                    runCatching { editor.abort() }
                }
            }
        }
        return SourceFetchResult(
            source = ImageSource(
                source = Buffer().apply { write(thumbnailData) },
                fileSystem = options.fileSystem,
            ),
            mimeType = null,
            dataSource = DataSource.DISK,
        )
    }

    private fun cachedResult(cache: DiskCache, key: String, snapshot: DiskCache.Snapshot) =
        SourceFetchResult(
            source = ImageSource(snapshot.data, cache.fileSystem, key, snapshot),
            mimeType = null,
            dataSource = DataSource.DISK,
        )

    class Factory : Fetcher.Factory<LocalMedia> {
        override fun create(data: LocalMedia, options: Options, imageLoader: ImageLoader): Fetcher {
            return LocalMediaFetcher(data, options, imageLoader)
        }
    }

    class UriFactory : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.scheme != null && !data.scheme.equals("file", ignoreCase = true)) return null
            val extension = data.path?.substringAfterLast('.')?.lowercase()
            if (extension !in MediaTypes.playableExtensions) return null
            // Raw media paths must not reach SkiaImageDecoder, which reads the entire file.
            return LocalMediaFetcher(LocalMedia(data.toString()), options, imageLoader)
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
    // Android already decodes video frames without loading the whole file into memory.
    if (platform != Platform.Android) add(LocalMediaFetcher.UriFactory())
}

expect fun getLocalMediaThumbnailData(filePath: String): ByteArray?

/**
 * Returns a cheap representation of the underlying file revision, or null when [filePath] cannot
 * be resolved to a regular file. This performs filesystem I/O and LocalMedia image requests must
 * run their interceptor coroutine context on an I/O dispatcher.
 */
expect fun getLocalMediaFileRevision(filePath: String): String?
