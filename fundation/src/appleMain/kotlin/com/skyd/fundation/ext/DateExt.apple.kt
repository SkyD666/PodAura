package com.skyd.fundation.ext

import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitSecond
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponentsFormatter
import platform.Foundation.NSDateComponentsFormatterUnitsStylePositional
import platform.Foundation.NSDateComponentsFormatterZeroFormattingBehaviorPad
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

actual fun Long.toAbsoluteDateTimeString(): String {
    val date = NSDate.dateWithTimeIntervalSince1970(this / 1000.0)
    val formatter = NSDateFormatter().apply {
        dateStyle = NSDateFormatterMediumStyle
        timeStyle = NSDateFormatterNoStyle
    }
    return formatter.stringFromDate(date)
}

actual fun Long.toRelativeDateTimeString(): String {
    val date = NSDate.dateWithTimeIntervalSince1970(this / 1000.0)
    val formatter = NSDateFormatter().apply {
        dateStyle = NSDateFormatterMediumStyle
        timeStyle = NSDateFormatterNoStyle
        doesRelativeDateFormatting = true
    }
    return formatter.stringFromDate(date)
}

actual fun Long.toShortDateString(): String {
    val date = NSDate.dateWithTimeIntervalSince1970(this / 1000.0)
    val formatter = NSDateFormatter().apply {
        setLocalizedDateFormatFromTemplate("Md")
    }
    return formatter.stringFromDate(date)
}

actual fun Long.toTimeString(): String {
    val date = NSDate.dateWithTimeIntervalSince1970(this / 1000.0)
    val formatter = NSDateFormatter().apply {
        dateStyle = NSDateFormatterNoStyle
        timeStyle = NSDateFormatterShortStyle
    }
    return formatter.stringFromDate(date)
}

actual fun Long.toWeekdayString(): String {
    val current = Clock.System.now().toEpochMilliseconds()
    val delta = current - this
    return if (delta < 2.days.inWholeMilliseconds) {
        this.toRelativeDateTimeString()
    } else {
        val date = NSDate.dateWithTimeIntervalSince1970(this / 1000.0)
        val formatter = NSDateFormatter().apply {
            setLocalizedDateFormatFromTemplate("EEE")
        }
        formatter.stringFromDate(date)
    }
}

actual fun Long.formatElapsedTime(): String {
    val formatter = NSDateComponentsFormatter().apply {
        unitsStyle = NSDateComponentsFormatterUnitsStylePositional
        allowedUnits = NSCalendarUnitHour or NSCalendarUnitMinute or NSCalendarUnitSecond
        zeroFormattingBehavior = NSDateComponentsFormatterZeroFormattingBehaviorPad
    }
    return formatter.stringFromTimeInterval(this / 1000.0) ?: "00:00"
}

actual fun is24HourStyle(): Boolean {
    val locale = NSLocale.currentLocale
    val pattern = NSDateFormatter.dateFormatFromTemplate(
        tmplate = "j",
        options = 0u,
        locale = locale
    ) ?: return false
    return !pattern.contains("a", ignoreCase = true)
}
