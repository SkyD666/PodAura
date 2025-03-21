package com.skyd.anivu.ext.content

import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore

fun ContentValues.gallery(
    fileNameWithExt: String,
    mimetype: String? = null,
) = apply {
    put(MediaStore.Images.Media.DISPLAY_NAME, fileNameWithExt)
    if (mimetype != null) {
        put(MediaStore.Images.Media.MIME_TYPE, mimetype)
    }
    put(
        MediaStore.Images.Media.RELATIVE_PATH,
        Environment.DIRECTORY_PICTURES + "/PodAura"
    )
}