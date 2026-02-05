package com.skyd.podaura.ext

import platform.Foundation.NSByteCountFormatter
import platform.Foundation.NSByteCountFormatterCountStyleFile
import platform.Foundation.NSString
import platform.Foundation.stringWithFormat

actual fun Long.fileSize(): String = NSByteCountFormatter.stringFromByteCount(
    byteCount = this,
    countStyle = NSByteCountFormatterCountStyleFile
)

actual fun Float.toPercentage(point: Int): String {
    require(point >= 0) { "Float.toPercentage error, point should be positive" }
    return NSString.stringWithFormat("%.${point}f%%", this * 100)
}
