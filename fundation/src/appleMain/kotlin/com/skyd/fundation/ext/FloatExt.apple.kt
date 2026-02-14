package com.skyd.fundation.ext

import platform.Foundation.NSString
import platform.Foundation.stringWithFormat

actual fun Float.format(point: Int): String {
    require(point >= 0) { "Float.format error, point should be positive" }
    return NSString.stringWithFormat("%.${point}f", this)
}
