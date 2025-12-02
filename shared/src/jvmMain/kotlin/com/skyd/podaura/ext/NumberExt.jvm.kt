package com.skyd.podaura.ext

import com.skyd.fundation.jna.mac.NSByteCountFormatter
import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.platform

actual fun Long.fileSize(): String {
    when (platform) {
        Platform.Android,
        Platform.IOS -> error("Not supported platform")

        Platform.Linux -> {
            TODO()
        }

        Platform.MacOS -> {
            return NSByteCountFormatter.stringFromByteCount(
                byteCount = this,
                countStyle = NSByteCountFormatter.CountStyle.FILE,
            )
        }

        Platform.Windows -> TODO()
    }
}

actual fun Float.toPercentage(point: Int): String {
    assert(point >= 0) { "Float.toPercentage error, point should be positive" }
    return "%.${point}f%%".format(this * 100)
}