package com.skyd.podaura.util.coil.localmedia

import android.media.MediaMetadataRetriever
import android.net.Uri
import co.touchlab.kermit.Logger
import java.io.File

actual fun getLocalMediaThumbnailData(filePath: String): ByteArray? {
    val retriever = MediaMetadataRetriever()
    return try {
        with(retriever) {
            setDataSource(filePath.toLocalMediaPath())
            embeddedPicture?.takeIf { it.isNotEmpty() }
        }
    } catch (e: Exception) {
        Logger.w(throwable = e, tag = "LocalMediaFetcher") {
            "Failed to extract embedded artwork from $filePath"
        }
        null
    } finally {
        retriever.release()
    }
}

actual fun getLocalMediaFileRevision(filePath: String): String? {
    val file = File(filePath.toLocalMediaPath())
    if (!file.isFile) return null
    return "${file.lastModified()}:${file.length()}"
}

private fun String.toLocalMediaPath(): String {
    return if (startsWith("file:", ignoreCase = true)) {
        Uri.parse(this).path ?: this
    } else {
        this
    }
}
