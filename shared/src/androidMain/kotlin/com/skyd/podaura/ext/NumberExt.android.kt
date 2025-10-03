package com.skyd.podaura.ext

import android.text.format.Formatter
import com.skyd.podaura.di.get

actual fun Long.fileSize(): String = Formatter.formatFileSize(get(), this)
    // On some systems, zero-width characters are inserted into the string
    // for unknown reasons 🤔, and they need to be removed to avoid display issues.
    .replace(Regex("[\\u200B\\u200C\\u200E\\u200F\\u202A-\\u202E\\u2060]"), "")

actual fun Float.toPercentage(point: Int): String {
    assert(point >= 0) { "Float.toPercentage error, point should be positive" }
    return "%.${point}f%%".format(this * 100)
}