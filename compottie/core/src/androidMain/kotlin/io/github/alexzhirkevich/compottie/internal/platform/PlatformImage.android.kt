package io.github.alexzhirkevich.compottie.internal.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

internal actual suspend fun ImageBitmap.Companion.fromBytes(
    bytes: ByteArray,
    width: Int,
    height: Int
): ImageBitmap {

//    return BitmapFactory
//        .decodeByteArray(bytes, 0, bytes.size)
//        .asImageBitmap()
//        .resize(width, height)

    return BitmapFactory
        .decodeByteArray(bytes, 0, bytes.size)
        .let {
            if (it.width == width && it.height == height) {
                it
            } else {
                Bitmap.createScaledBitmap(it, width, height, false)
            }
        }.asImageBitmap()
}
