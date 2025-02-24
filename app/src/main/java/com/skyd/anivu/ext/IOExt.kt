package com.skyd.anivu.ext

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.webkit.URLUtil
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import com.skyd.anivu.R
import com.skyd.anivu.appContext
import com.skyd.anivu.ui.component.showToast
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.LinkedList


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

    return name ?: path?.substringAfterLast("/")?.toDecodedUrl()
}

val Uri.type: String?
    get() = appContext.contentResolver.getType(this)

fun String.openBrowser(context: Context) {
    Uri.parse(this).openBrowser(context)
}

fun Uri.openBrowser(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, this)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        e.printStackTrace()
        context.getString(R.string.no_browser_found, path).showToast(Toast.LENGTH_LONG)
    }
}

fun Uri.openWith(context: Context) = openChooser(
    context = context,
    action = Intent.ACTION_VIEW,
    chooserTitle = context.getString(R.string.open_with),
)

fun Uri.share(context: Context, mimeType: String? = null) = openChooser(
    context = context,
    action = Intent.ACTION_SEND,
    chooserTitle = context.getString(R.string.share),
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
        context.getString(R.string.failed_msg, e.message).showToast(Toast.LENGTH_LONG)
    }
}

fun Uri.toDocumentFile(context: Context, isTree: Boolean = false): DocumentFile? {
    val uriString = toString()
    return if (URLUtil.isFileUrl(uriString) || uriString.startsWith("/")) {
        DocumentFile.fromFile(File(uriString))
    } else {
        if (isTree) DocumentFile.fromTreeUri(context, this)
        else DocumentFile.fromSingleUri(context, this)
    }
}

fun Uri.traverseDirectoryEntries(
    contentResolver: ContentResolver,
    projection: Array<String>,
    onEach: (Uri, Cursor) -> Unit
) {
    var childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
        this,
        DocumentsContract.getTreeDocumentId(this)
    )

    // Keep track of our directory hierarchy
    val dirNodes: MutableList<Uri> = LinkedList()
    dirNodes.add(childrenUri)

    while (dirNodes.isNotEmpty()) {
        childrenUri = dirNodes.removeAt(0) // get the item from top
        contentResolver.query(
            childrenUri,
            (arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ) + projection).distinct().toTypedArray(),
            null,
            null,
            null
        )?.use { c ->
            while (c.moveToNext()) {
                val docId = c.getString(0)
                val mime = c.getString(1)
                if (DocumentsContract.Document.MIME_TYPE_DIR == mime) {
                    val newNode = DocumentsContract.buildChildDocumentsUriUsingTree(this, docId)
                    dirNodes.add(newNode)
                }
                onEach(childrenUri, c)
            }
        }
    }
}

fun Uri.listFiles(
    contentResolver: ContentResolver,
): List<Uri> {
    val result = mutableListOf<Uri>()

    contentResolver.query(
        this,
        arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
        null,
        null,
        null
    )?.use { c ->
        while (c.moveToNext()) {
            val docId = c.getString(0)
            result.add(DocumentsContract.buildDocumentUriUsingTree(this, docId))
        }
    }

    return result
}

/**
 * Needs tree uri
 */
fun Uri.findFile(contentResolver: ContentResolver, name: String): Uri? {
    contentResolver.query(
        this,
        arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        ),
        null,
        null,
        null
    )?.use { c ->
        while (c.moveToNext()) {
            val docId = c.getString(0)
            if (name != c.getString(1)) continue
            return DocumentsContract.buildDocumentUriUsingTree(this, docId)
        }
    }
    return null
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

fun File.md5(): String? {
    var bi: BigInteger? = null
    runCatching {
        val buffer = ByteArray(4096)
        var len: Int
        val md = MessageDigest.getInstance("MD5")
        FileInputStream(this).use { fis ->
            while (fis.read(buffer).also { len = it } != -1) {
                md.update(buffer, 0, len)
            }
        }
        val b = md.digest()
        bi = BigInteger(1, b)
    }.onFailure {
        when (it) {
            is NoSuchAlgorithmException -> it.printStackTrace()
            is IOException -> it.printStackTrace()
            else -> throw it
        }
    }
    return bi?.toString(16)
}

inline val String.extName: String
    get() = substringAfterLast(".", missingDelimiterValue = "")

fun Uri.copyToClipboard(context: Context, mimeType: String? = null) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(
        ClipData(context.applicationInfo.name, arrayOf(mimeType), ClipData.Item(this))
    )
    // If you show a copy confirmation toast in Android 13, the user sees duplicate messages.
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
        context.getString(R.string.copied).showToast()
    }
}