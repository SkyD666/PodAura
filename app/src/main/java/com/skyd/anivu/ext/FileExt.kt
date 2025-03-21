package com.skyd.anivu.ext

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.skyd.anivu.R
import com.skyd.anivu.config.Const.PODAURA_PICTURES_DIR
import com.skyd.anivu.ext.content.saveToGallery
import com.skyd.anivu.ui.component.showToast
import java.io.File


fun File.toUri(context: Context): Uri {
    return try {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", this)
    } catch (e: IllegalArgumentException) {
        toUri()
    }
}

fun File.deleteRecursivelyExclude(hook: (File) -> Boolean = { true }): Boolean =
    walkBottomUp().fold(true) { res, it ->
        (it != this && hook(it) && (it.delete() || !it.exists())) && res
    }

fun File.deleteDirs(
    maxSize: Int = 5_242_880,
    exclude: (file: File) -> Boolean,
) {
    if (walkTopDown().filter { it.isFile }.map { it.length() }.sum() > maxSize) {
        walkBottomUp().forEach { if (!exclude(it)) it.deleteRecursively() }
    }
}

fun File.getMimeType(): String? {
    if (isDirectory) return DocumentsContract.Document.MIME_TYPE_DIR
    var type: String? = null
    val extension = path.substringAfterLast(".", "")
    if (extension.isNotBlank()) {
        type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }
    return type
}

fun File.savePictureToMediaStore(
    context: Context,
    mimetype: String? = null,
    fileName: String = name,
    autoDelete: Boolean = true,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        context.contentResolver.saveToGallery(
            fileNameWithExt = fileName,
            mimetype = mimetype,
            output = { output ->
                inputStream().use { input -> input.copyTo(output) }
                true
            }
        )
    } else {
        this.copyTo(File(PODAURA_PICTURES_DIR, fileName))
    }
    context.getString(R.string.save_picture_to_media_store_saved).showToast()
    if (autoDelete) delete()
}