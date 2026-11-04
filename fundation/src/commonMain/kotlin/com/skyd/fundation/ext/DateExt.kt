package com.skyd.fundation.ext

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.parseOrNull
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant


expect fun Long.toAbsoluteDateTimeString(): String

expect fun Long.toRelativeDateTimeString(): String

expect fun Long.toShortDateString(): String

expect fun Long.toTimeString(): String

expect fun Long.toWeekdayString(): String

fun Clock.Companion.currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

expect fun Long.formatElapsedTime(): String

fun Instant.Companion.tryParse(input: CharSequence): Instant? =
    parseOrNull(input, DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET)
        ?: parseOrNull(input, DateTimeComponents.Formats.RFC_1123)

fun Long.nextMidnight(timeZone: TimeZone = TimeZone.currentSystemDefault()): Long {
    val instant = Instant.fromEpochMilliseconds(this)
    val date = instant.toLocalDateTime(timeZone).date
    val nextDate = date.plus(1, DateTimeUnit.DAY)
    return nextDate.atStartOfDayIn(timeZone).toEpochMilliseconds()
}

val Long.hour: Int
    get() {
        val instant = Instant.fromEpochMilliseconds(this)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return localDateTime.hour
    }

val Long.hourWithStyle: Int
    get() = hour.withStyle

val Int.withStyle: Int
    get() {
        return if (is24HourStyle()) {
            this
        } else {
            if (this <= 12) {
                this
            } else {
                this - 12
            }
        }
    }

fun Int.timestampForTodayHour(): Long {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val dateTime = LocalDateTime(today, LocalTime(this, 0))
    return dateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
}

expect fun is24HourStyle(): Boolean
