package com.skyd.anivu.ext.content

import android.content.ContentResolver
import android.content.ContentValues
import android.provider.MediaStore
import java.io.OutputStream

fun ContentResolver.saveToGallery(
    fileNameWithExt: String,
    mimetype: String? = null,
    output: (OutputStream) -> Boolean,
): Boolean {
    val contentValues = ContentValues().gallery(fileNameWithExt, mimetype)
    val uri = insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ) ?: return false
    return openOutputStream(uri)?.use(output) == true
}