package com.skyd.podaura.ext

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.skyd.podaura.config.Const
import com.skyd.podaura.config.PODAURA_PICTURES_DIR
import com.skyd.podaura.ext.content.saveToGallery
import com.skyd.podaura.ui.component.showToast
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.save_picture_to_media_store_saved
import java.io.File


fun File.toUri(context: Context): Uri {
    return try {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", this)
    } catch (_: IllegalArgumentException) {
        toUri()
    }
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
        this.copyTo(File(Const.PODAURA_PICTURES_DIR, fileName))
    }
    runBlocking { getString(Res.string.save_picture_to_media_store_saved) }.showToast()
    if (autoDelete) delete()
}