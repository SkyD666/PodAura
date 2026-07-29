package com.skyd.podaura.util.coil.localmedia

import co.touchlab.kermit.Logger
import org.jaudiotagger.audio.AudioFileIO
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.logging.Level

actual fun getLocalMediaThumbnailData(filePath: String): ByteArray? {
    return try {
        JaudioTaggerLogging.ensureConfigured()
        AudioFileIO.read(filePath.toLocalMediaFile())
            .tag
            ?.firstArtwork
            ?.binaryData
            ?.takeIf { it.isNotEmpty() }
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
