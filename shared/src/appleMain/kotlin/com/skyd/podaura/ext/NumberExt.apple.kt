package com.skyd.podaura.ext

import com.skyd.fundation.ext.format
import platform.Foundation.NSByteCountFormatter
import platform.Foundation.NSByteCountFormatterCountStyleFile

actual fun Long.fileSize(): String = NSByteCountFormatter.stringFromByteCount(
    byteCount = this,
    countStyle = NSByteCountFormatterCountStyleFile
)

actual fun Float.toPercentage(point: Int): String {
    require(point >= 0) { "Float.toPercentage error, point should be positive" }
    return (this * 100).format(point) + "%"
}
