package com.skyd.fundation.ext

import java.util.Locale

actual fun Float.format(point: Int): String {
    assert(point >= 0) { "Float.format error, point should be positive" }
    return String.format(Locale.getDefault(), "%.${point}f", this)
}