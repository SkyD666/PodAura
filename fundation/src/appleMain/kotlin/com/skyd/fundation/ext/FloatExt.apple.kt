package com.skyd.fundation.ext

import com.skyd.fundation.locale.currentFormattingLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.numberWithFloat

actual fun Float.format(point: Int): String {
    require(point >= 0) { "Float.format error, point should be positive" }
    return NSNumberFormatter().run {
        locale = currentFormattingLocale()
        usesGroupingSeparator = false
        minimumFractionDigits = point.toULong()
        maximumFractionDigits = point.toULong()
        stringFromNumber(NSNumber.numberWithFloat(this@format)) ?: this@format.toString()
    }
}
