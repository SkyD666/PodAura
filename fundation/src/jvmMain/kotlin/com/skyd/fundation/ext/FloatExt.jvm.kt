package com.skyd.fundation.ext

import com.skyd.fundation.locale.currentFormattingLocale

actual fun Float.format(point: Int): String {
    require(point >= 0) { "Float.format error, point should be positive" }
    return String.format(currentFormattingLocale(), "%.${point}f", this)
}
