package com.skyd.fundation.util

import com.sun.jna.platform.win32.Kernel32
import kotlin.system.exitProcess

actual val platform: Platform
    get() {
        val system = System.getProperty("os.name").lowercase()
        return when {
            system.contains("win") -> Platform.Windows
            arrayOf("nix", "nux", "aix").any { system.contains(it) } -> Platform.Linux
            system.contains("mac") -> Platform.MacOS
            else -> Platform.Linux
        }
    }

object WindowsUtil {

    val buildNumber: Int? by lazy {
        if (platform != Platform.Windows) return@lazy null
        Kernel32.INSTANCE.GetVersion().high.toInt()
    }

    fun isWindows11OrLater(): Boolean = buildNumber?.let { it >= 22000 } ?: false
}

actual fun exitApp() {
    exitProcess(0)
}
