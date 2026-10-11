package com.skyd.podaura.util.coil.localmedia

import co.touchlab.kermit.Logger
import com.skyd.podaura.media.MediaTypes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.jaudiotagger.audio.AudioFileIO
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.logging.Level

actual suspend fun getLocalMediaThumbnailData(filePath: String): ByteArray? {
    currentCoroutineContext().ensureActive()
    return try {
        val file = filePath.toLocalMediaFile()
        if (file.extension.lowercase() in MediaTypes.videoExtensions) {
            return getFfmpegArtworkData(file.absolutePath)
                ?: getLocalVideoThumbnailData(file.absolutePath)
        }
        JaudioTaggerLogging.ensureConfigured()
        AudioFileIO.read(file)
            .tag
            ?.firstArtwork
            ?.binaryData
            ?.takeIf { it.isNotEmpty() }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Logger.w(throwable = e, tag = "LocalMediaFetcher") {
            "Failed to extract embedded artwork from $filePath"
        }
        null
    }
}

actual fun getLocalMediaFileRevision(filePath: String): String? {
    val file = runCatching { filePath.toLocalMediaFile() }.getOrNull() ?: return null
    val attributes = runCatching {
        Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
    }.getOrNull() ?: return null
    if (!attributes.isRegularFile) return null
    return "${attributes.lastModifiedTime().toMillis()}:${attributes.size()}"
}

private fun String.toLocalMediaFile(): File {
    return if (startsWith("file:", ignoreCase = true)) {
        Paths.get(URI(this)).toFile()
    } else {
        File(this)
    }
}

private object JaudioTaggerLogging {
    init {
        java.util.logging.Logger.getLogger("org.jaudiotagger").level = Level.WARNING
    }

    fun ensureConfigured() = Unit
}
