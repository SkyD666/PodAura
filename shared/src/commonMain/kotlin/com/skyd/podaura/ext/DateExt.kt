package com.skyd.podaura.ext

import co.touchlab.kermit.Logger
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.parse
import kotlin.time.Clock
import kotlin.time.Instant


expect fun Long.toDateTimeString(): String

expect fun Long.toAbsoluteDateTimeString(): String

expect fun Long.toRelativeDateTimeString(): String

fun Clock.Companion.currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

expect fun Long.formatElapsedTime(): String

fun Instant.Companion.tryParse(input: CharSequence): Instant? {
    val formats = listOf(
        DateTimeComponents.Formats.RFC_1123,
        DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET,
    )
    formats.forEach { format ->
        val instant = runCatching {
            parse(input, format)
        }.onFailure {
            Logger.w("Instant.Companion.tryParse", it)
        }.getOrNull()
        if (instant != null) {
            return instant
        }
    }
    return null
}

fun LocalTime.Companion.tryParse(input: CharSequence): LocalTime? {
    val formats = listOf(
        LocalTime.Formats.ISO,
        LocalTime.Formats.SECONDS,
    )
    formats.forEach { format ->
        val time = runCatching {
            parse(input, format)
        }.onFailure {
            Logger.w("Instant.Companion.tryParse", it)
        }.getOrNull()
        if (time != null) {
            return time
        }
    }
    return null
}

@Suppress("UnusedReceiverParameter")
val LocalTime.Formats.SECONDS
    get() = LocalTime.Format { second() }