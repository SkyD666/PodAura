package com.skyd.podaura.ext

import kotlin.time.Clock


expect fun Long.toDateTimeString(): String

expect fun Long.toAbsoluteDateTimeString(): String

expect fun Long.toRelativeDateTimeString(): String

fun Clock.Companion.currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

expect fun Long.formatElapsedTime(): String