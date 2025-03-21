package com.skyd.anivu.ext

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import com.skyd.anivu.config.Const
import com.skyd.anivu.ext.content.saveToGallery
import kotlin.random.Random

fun BitmapDrawable.saveToGallery(
    context: Context,
    filename: String = System.currentTimeMillis().toString() + "_" + Random.nextInt(),
): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        context.contentResolver.saveToGallery(
            fileNameWithExt = "$filename.png",
            mimetype = "image/png",
            output = { outputStream ->
                bitmap?.compress(Bitmap.CompressFormat.PNG, 100, outputStream) == true
            }
        )
    } else {
        Const.PODAURA_PICTURES_DIR.outputStream().use { outputStream ->
            bitmap?.compress(Bitmap.CompressFormat.PNG, 100, outputStream) == true
        }
    }
}