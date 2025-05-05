package com.skyd.podaura.ext

import android.webkit.URLUtil
import androidx.core.net.toUri
import io.github.vinceglb.filekit.PlatformFile

actual fun String.isLocalFile(): Boolean = startsWith("/") ||
        startsWith("fd://") ||
        URLUtil.isFileUrl(this) ||
        URLUtil.isContentUrl(this)

actual fun String.asPlatformFile(): PlatformFile {
    return if (startsWith("content://")) PlatformFile(toUri())
    else PlatformFile(this)
}

actual fun String.isNetworkUrl(): Boolean = URLUtil.isNetworkUrl(toString())