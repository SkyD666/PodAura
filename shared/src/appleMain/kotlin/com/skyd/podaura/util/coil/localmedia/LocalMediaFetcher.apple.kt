package com.skyd.podaura.util.coil.localmedia

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFoundation.AVMetadataCommonIdentifierArtwork
import platform.AVFoundation.AVMetadataItem
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.commonMetadata
import platform.AVFoundation.metadataItemsFromArray
import platform.Foundation.NSDate
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileModificationDate
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.getBytes
import platform.Foundation.longLongValue
import platform.Foundation.timeIntervalSince1970

@OptIn(ExperimentalForeignApi::class)
actual fun getLocalMediaThumbnailData(filePath: String): ByteArray? {
    val fileUrl = filePath.toLocalMediaUrl() ?: return null
    val asset = AVURLAsset(
        uRL = fileUrl,
        options = null,
    )
    val artwork = AVMetadataItem.metadataItemsFromArray(
        metadataItems = asset.commonMetadata,
        filteredByIdentifier = AVMetadataCommonIdentifierArtwork,
    ).firstOrNull() as? AVMetadataItem
    val data = artwork?.value as? NSData ?: return null
    return data.toByteArray().takeIf { it.isNotEmpty() }
}

@OptIn(ExperimentalForeignApi::class)
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

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
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
