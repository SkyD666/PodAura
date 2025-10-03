package com.skyd.podaura.ext

import android.os.ParcelFileDescriptor
import android.webkit.URLUtil
import androidx.core.net.toUri
import io.github.vinceglb.filekit.PlatformFile
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

actual fun String.isLocalFile(): Boolean = startsWith("/") ||
        startsWith("fd://") ||
        URLUtil.isFileUrl(this) ||
        URLUtil.isContentUrl(this)

actual fun String.asPlatformFile(): PlatformFile {
    return if (startsWith("content://")) PlatformFile(toUri())
    else PlatformFile(this)
}

actual fun String.isNetworkUrl(): Boolean = URLUtil.isNetworkUrl(toString())

fun isFdFileExists(fdPath: String): Boolean {
    var pfd: ParcelFileDescriptor? = null
    var inputStream: InputStream? = null
    return try {
        val fd = fdPath.substringAfterLast("fd://").toInt()
        pfd = ParcelFileDescriptor.fromFd(fd)
        inputStream = FileInputStream(pfd.fileDescriptor)
        inputStream.read() != -1
    } catch (e: IOException) {
        e.printStackTrace()
        false
    } finally {
        inputStream?.close()
        pfd?.close()
    }
}

actual fun String.isLocalFileExists(): Boolean {
    if (!isLocalFile()) return false
    return startsWith("fd://") && isFdFileExists(this) || File(this).exists()
}