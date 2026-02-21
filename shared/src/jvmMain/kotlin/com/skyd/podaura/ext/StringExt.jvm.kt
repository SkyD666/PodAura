package com.skyd.podaura.ext

import io.github.vinceglb.filekit.PlatformFile
import java.io.File

actual fun String.isLocalFile(): Boolean = startsWith("/") ||
        startsWith("fd://") ||
        startsWith("file:")

actual fun String.asPlatformFile(): PlatformFile = PlatformFile(this)

actual fun String.isNetworkUrl(): Boolean {
    return (length > 6) && substring(0, 7).equals("http://", ignoreCase = true) ||
            (length > 7) && substring(0, 8).equals("https://", ignoreCase = true)
}

actual fun String.isLocalFileExists(): Boolean {
    if (!isLocalFile()) return false
    return File(this).exists()
}