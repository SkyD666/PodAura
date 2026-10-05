package com.skyd.podaura.model.bean.playlist

import platform.AVFoundation.AVMetadataCommonIdentifierArtist
import platform.AVFoundation.AVMetadataCommonIdentifierTitle
import platform.AVFoundation.AVMetadataItem
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.commonMetadata
import platform.AVFoundation.metadataItemsFromArray
import platform.AVFoundation.stringValue
import platform.CoreMedia.CMTimeGetSeconds
import platform.Foundation.NSURL

actual fun PlaylistMediaBean.updateLocalMediaMetadata() {
    if (!isLocalFile) return
    // mpv remains the playback engine; AVFoundation only reads optional local metadata.
    runCatching {
        val location =
            if (url.startsWith("file:")) NSURL.URLWithString(url) else NSURL.fileURLWithPath(url)
        val asset = AVURLAsset(requireNotNull(location), null)
        fun text(identifier: String?): String? =
            (AVMetadataItem.metadataItemsFromArray(asset.commonMetadata, identifier)
                .firstOrNull() as? AVMetadataItem)?.stringValue
        title = text(AVMetadataCommonIdentifierTitle)
        artist = text(AVMetadataCommonIdentifierArtist)
        duration = CMTimeGetSeconds(asset.duration).takeIf { it.isFinite() && it >= 0 }?.times(1000)
            ?.toLong()
    }
}
