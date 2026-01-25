package com.skyd.fundation.ext

import com.skyd.fundation.jna.mac.NSCalendar
import com.skyd.fundation.jna.mac.NSDate
import com.skyd.fundation.jna.mac.NSDateComponentsFormatter
import com.skyd.fundation.jna.mac.NSDateFormatter
import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.notSupport
import com.skyd.fundation.util.platform
import org.ocpsoft.prettytime.PrettyTime
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.TimeZone


actual fun Long.toAbsoluteDateTimeString(): String {
    when (platform) {
        Platform.Android,
        Platform.IOS -> notSupport("toAbsoluteDateTimeString")

        Platform.Windows,
        Platform.Linux -> {
            val formatter = DateTimeFormatter
                .ofLocalizedDate(FormatStyle.LONG)
                .withLocale(Locale.getDefault())
            return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(this),
                TimeZone.getDefault().toZoneId()
            ).format(formatter)
        }

        Platform.MacOS -> {
            val date = NSDate.dateWithTimeIntervalSince1970(this / 1000.0)
            val formatter = NSDateFormatter().apply {
                setDateStyle(NSDateFormatter.Style.MEDIUM)
                setTimeStyle(NSDateFormatter.Style.NO)
            }
            return formatter.stringFromDate(date)
        }
    }
}

actual fun Long.toRelativeDateTimeString(): String {
    when (platform) {
        Platform.Android,
        Platform.IOS -> notSupport("toRelativeDateTimeString")

        Platform.Windows,
        Platform.Linux -> {
            val formatter = DateTimeFormatter
                .ofLocalizedDate(FormatStyle.LONG)
                .withLocale(Locale.getDefault())
            return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(this),
                TimeZone.getDefault().toZoneId()
            ).format(formatter)
        }

        Platform.MacOS -> {
            val date = NSDate.dateWithTimeIntervalSince1970(this / 1000.0)
            val formatter = NSDateFormatter().apply {
                setDateStyle(NSDateFormatter.Style.MEDIUM)
                setTimeStyle(NSDateFormatter.Style.NO)
                setDoesRelativeDateFormatting(true)
            }
            return formatter.stringFromDate(date)
        }
    }
}

actual fun Long.formatElapsedTime(): String {
    return when (platform) {
        Platform.Android,
        Platform.IOS -> notSupport("formatElapsedTime")

        Platform.Windows,
        Platform.Linux -> PrettyTime().format(
            LocalDateTime.ofInstant(
                Instant.ofEpochMilli(this),
                TimeZone.getDefault().toZoneId()
            )
        )

        Platform.MacOS -> {
            val formatter = NSDateComponentsFormatter().apply {
                setUnitsStyle(NSDateComponentsFormatter.UnitsStyle.POSITIONAL)
                setAllowedUnits(
                    NSCalendar.Unit.HOUR or NSCalendar.Unit.MINUTE or NSCalendar.Unit.SECOND
                )
                setZeroFormattingBehavior(NSDateComponentsFormatter.ZeroFormattingBehavior.BEHAVIOR_PAD)
            }
            formatter.stringFromTimeInterval(this / 1000.0)
        }

    }
}