package com.skyd.podaura.util.coil.localmedia

import com.skyd.podaura.media.MediaTypes
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFoundation.AVMetadataCommonIdentifierArtwork
import platform.AVFoundation.AVMetadataItem
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.availableMetadataFormats
import platform.AVFoundation.commonMetadata
import platform.AVFoundation.dataValue
import platform.AVFoundation.metadataForFormat
import platform.AVFoundation.metadataItemsFromArray
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileModificationDate
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.getBytes
import platform.Foundation.timeIntervalSince1970

actual fun getLocalMediaThumbnailData(filePath: String): ByteArray? {
    val fileUrl = filePath.toLocalMediaUrl() ?: return null
    val extension = fileUrl.pathExtension?.lowercase()
    if (extension in MediaTypes.videoExtensions) {
        getLocalVideoThumbnailData(fileUrl.path ?: return null)?.let { return it }
    }
    return getAppleArtworkData(fileUrl) ?: getFfmpegArtworkData(fileUrl.path ?: return null)
}

internal fun getAppleArtworkData(fileUrl: NSURL): ByteArray? {
    val asset = AVURLAsset(uRL = fileUrl, options = null)
    // FLAC and Vorbis artwork can be absent from commonMetadata but present in a format's metadata.
    val metadata = sequence {
        yield(asset.commonMetadata)
        for (format in asset.availableMetadataFormats) {
            yield(asset.metadataForFormat(format as String))
        }
    }
    for (items in metadata) {
        for (item in AVMetadataItem.metadataItemsFromArray(items, AVMetadataCommonIdentifierArtwork)) {
            val bytes = (item as? AVMetadataItem)?.dataValue?.toByteArray()
            if (bytes != null && bytes.isNotEmpty()) return bytes
        }
    }
    return null
}

actual fun getLocalMediaFileRevision(filePath: String): String? {
    val path = filePath.toLocalMediaUrl()?.path ?: return null
    val attributes = NSFileManager.defaultManager
        .attributesOfItemAtPath(path = path, error = null)
        ?: return null
    val modifiedAt = (attributes[NSFileModificationDate] as? NSDate)
        ?.timeIntervalSince1970
        ?.times(1_000)
        ?.toLong()
        ?: return null
    val size = (attributes[NSFileSize] as? NSNumber)?.longLongValue ?: return null
    return "$modifiedAt:$size"
}

private fun String.toLocalMediaUrl(): NSURL? {
    return if (startsWith("file:", ignoreCase = true)) {
        NSURL(string = this)
    } else {
        NSURL.fileURLWithPath(this)
    }
}

internal fun NSData.toByteArray(): ByteArray {
    require(length <= Int.MAX_VALUE.toULong()) {
        "Embedded artwork is too large: $length bytes"
    }
    return ByteArray(length.toInt()).also { bytes ->
        if (bytes.isNotEmpty()) {
            bytes.usePinned { pinned ->
                getBytes(pinned.addressOf(0), length)
            }
        }
    }
}
