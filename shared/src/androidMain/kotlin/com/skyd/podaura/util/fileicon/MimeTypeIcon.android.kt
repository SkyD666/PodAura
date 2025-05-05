package com.skyd.podaura.util.fileicon

import android.webkit.MimeTypeMap
import com.skyd.podaura.ext.extension
import com.skyd.podaura.ext.isDirectory
import kotlinx.io.files.Path

actual fun Path.mimeType(): String? {
    if (isDirectory == true) return null
    var type: String? = null
    if (extension.isNotBlank()) {
        type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }
    return type
}