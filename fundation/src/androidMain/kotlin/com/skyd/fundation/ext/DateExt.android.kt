package com.skyd.fundation.ext

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.Locale

actual fun Long.toAbsoluteDateTimeString(): String = SimpleDateFormat
    .getDateTimeInstance(SimpleDateFormat.MEDIUM, SimpleDateFormat.MEDIUM, Locale.getDefault())
    .format(this)

actual fun Long.toRelativeDateTimeString(): String {
    val current = System.currentTimeMillis()
    val delta = current - this
    return DateUtils.getRelativeTimeSpanString(
        this,
        current,
        // "DateUtils.WEEK_IN_MILLIS <= .. <= DateUtils.WEEK_IN_MILLIS * 4" is 1~3 weeks ago
        if (delta in DateUtils.WEEK_IN_MILLIS..DateUtils.WEEK_IN_MILLIS * 4) {
            DateUtils.WEEK_IN_MILLIS
        } else 0
    ).toString()
}

actual fun Long.formatElapsedTime(): String {
    return DateUtils.formatElapsedTime(this / 1000)
}