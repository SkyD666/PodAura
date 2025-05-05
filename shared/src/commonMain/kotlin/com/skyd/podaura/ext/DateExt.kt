package com.skyd.podaura.ext

import kotlinx.datetime.Clock

expect fun Long.toDateTimeString(): String

expect fun Long.toAbsoluteDateTimeString(): String

expect fun Long.toRelativeDateTimeString(): String

fun Clock.Companion.currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()