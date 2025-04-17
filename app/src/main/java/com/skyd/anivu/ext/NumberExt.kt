package com.skyd.anivu.ext

import android.content.Context
import android.text.format.Formatter

fun Long.fileSize(context: Context): String =
    Formatter.formatFileSize(context, this)
        // On some systems, zero-width characters are inserted into the string
        // for unknown reasons 🤔, and they need to be removed to avoid display issues.
        .replace(Regex("[\\u200B\\u200C\\u200E\\u200F\\u202A-\\u202E\\u2060]"), "")

fun Float.toPercentage(format: String = "%.2f%%"): String = format.format(this * 100)

fun Float.toDegrees(): Float = (this * 180 / Math.PI).toFloat()

fun Int.toSignedString(): String = if (this >= 0) "+$this" else toString()