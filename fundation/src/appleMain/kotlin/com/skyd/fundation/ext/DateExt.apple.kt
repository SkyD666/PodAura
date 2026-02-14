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
import platform.Foundation.dateWithTimeIntervalSince1970

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

actual fun Long.formatElapsedTime(): String {
    val formatter = NSDateComponentsFormatter().apply {
        unitsStyle = NSDateComponentsFormatterUnitsStylePositional
        allowedUnits = NSCalendarUnitHour or NSCalendarUnitMinute or NSCalendarUnitSecond
        zeroFormattingBehavior = NSDateComponentsFormatterZeroFormattingBehaviorPad
    }
    return formatter.stringFromTimeInterval(this / 1000.0) ?: "00:00"
}
