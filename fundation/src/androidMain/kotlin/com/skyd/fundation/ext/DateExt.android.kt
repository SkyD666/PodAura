package com.skyd.fundation.ext

import android.text.format.DateFormat
import android.text.format.DateUtils
import com.skyd.fundation.di.get
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

actual fun Long.toShortDateString(): String {
    val locale = Locale.getDefault()
    val pattern = DateFormat.getBestDateTimePattern(locale, "Md")
    return SimpleDateFormat(pattern, locale).format(this)
}

actual fun Long.toTimeString(): String {
    return DateFormat.getTimeFormat(get()).format(this)
}

actual fun Long.toWeekdayString(): String {
    val formatter = SimpleDateFormat("EEE", Locale.getDefault())
    return formatter.format(this)
}