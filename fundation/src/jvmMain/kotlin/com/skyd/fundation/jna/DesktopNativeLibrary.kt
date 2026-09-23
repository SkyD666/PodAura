package com.skyd.fundation.jna

import com.sun.jna.Native
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** Media and notifications must share one extracted binary, including its native globals. */
internal object DesktopNativeLibrary {
    private val files = ConcurrentHashMap<String, File>()

    fun file(baseName: String, fileName: String): File = files.computeIfAbsent(baseName) {
        System.getProperty("compose.application.resources.dir")
            ?.takeIf(String::isNotBlank)
            ?.let { File(it, fileName) }
            ?.takeIf(File::isFile)
            ?: Native.extractFromResourcePath(baseName, DesktopNativeLibrary::class.java.classLoader)
    }
}
