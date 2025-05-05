package com.skyd.podaura.util.coil.localmedia

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import coil3.Image
import coil3.asImage

actual fun getLocalMediaThumbnail(filePath: String): Image? {
    val retriever = MediaMetadataRetriever()
    return try {
        with(retriever) {
            setDataSource(filePath)
            embeddedPicture?.let {
                BitmapFactory.decodeByteArray(it, 0, it.size).asImage()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } finally {
        retriever.release()
    }
}