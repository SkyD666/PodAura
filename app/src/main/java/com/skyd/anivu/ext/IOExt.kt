package com.skyd.anivu.ext

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.webkit.URLUtil
import android.widget.Toast
import com.skyd.anivu.appContext
import com.skyd.anivu.ui.component.showToast
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.copied
import podaura.shared.generated.resources.failed_msg
import podaura.shared.generated.resources.open_with
import podaura.shared.generated.resources.share
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


fun Uri.copyTo(target: File): Long {
    return appContext.contentResolver.openInputStream(this)!!.use { it.saveTo(target) }
}

fun Uri.fileName(): String? {
    var name: String? = null
    runCatching {
        appContext.contentResolver.query(
            this, null, null, null, null
        )?.use { cursor ->
            /*
             * Get the column indexes of the data in the Cursor,
             * move to the first row in the Cursor, get the data.
             */
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            name = cursor.getString(nameIndex)
        }
    }.onFailure { it.printStackTrace() }

    return name ?: path?.substringAfterLast("/")?.decodeURL()
}

val Uri.type: String?
    get() = appContext.contentResolver.getType(this)

fun Uri.openWith(context: Context) = openChooser(
    context = context,
    action = Intent.ACTION_VIEW,
    chooserTitle = context.getString(Res.string.open_with),
)

fun Uri.share(context: Context, mimeType: String? = null) = openChooser(
    context = context,
    action = Intent.ACTION_SEND,
    chooserTitle = context.getString(Res.string.share),
    mimeType = mimeType,
)

private fun Uri.openChooser(
    context: Context,
    action: String,
    chooserTitle: CharSequence,
    mimeType: String? = null,
) {
    try {
        val currentMimeType = mimeType ?: context.contentResolver.getType(this)
        val intent = Intent.createChooser(
            Intent().apply {
                this.action = action
                putExtra(Intent.EXTRA_STREAM, this@openChooser)
                setDataAndType(this@openChooser, currentMimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            chooserTitle
        )
        if (context.tryActivity == null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent, null)
    } catch (e: Exception) {
        e.printStackTrace()
        context.getString(Res.string.failed_msg, e.message!!).showToast(Toast.LENGTH_LONG)
    }
}

fun Uri.isLocal(): Boolean = toString().startsWith("/") ||
        toString().startsWith("fd://") ||
        URLUtil.isFileUrl(toString()) ||
        URLUtil.isContentUrl(toString())

fun String.isLocalFile(): Boolean = startsWith("/") ||
        startsWith("fd://") ||
        URLUtil.isFileUrl(this) ||
        URLUtil.isContentUrl(this)

fun Uri.isNetwork(): Boolean = URLUtil.isNetworkUrl(toString())

fun InputStream.saveTo(target: File): Long {
    val parentFile = target.parentFile
    if (parentFile?.exists() == false) {
        parentFile.mkdirs()
    }
    if (!target.exists()) {
        target.createNewFile()
    }
    return FileOutputStream(target).use { copyTo(it) }
}

fun Uri.copyToClipboard(context: Context, mimeType: String? = null) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(
        ClipData(context.applicationInfo.name, arrayOf(mimeType), ClipData.Item(this))
    )
    // If you show a copy confirmation toast in Android 13, the user sees duplicate messages.
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
        context.getString(Res.string.copied).showToast()
    }
}