package com.skyd.podaura.model.bean.playlist

import android.media.MediaMetadataRetriever

actual fun PlaylistMediaBean.updateLocalMediaMetadata() {
    val retriever = MediaMetadataRetriever()
    try {
        with(retriever) {
            setDataSource(url)
            duration =
                extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            title = extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            artist = extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    retriever.release()
}