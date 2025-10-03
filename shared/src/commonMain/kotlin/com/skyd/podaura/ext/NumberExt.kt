package com.skyd.podaura.ext

import kotlin.math.PI

fun Float.toDegrees(): Float = (this * 180 / PI).toFloat()

fun Int.toSignedString(): String = if (this >= 0) "+$this" else toString()

expect fun Long.fileSize(): String

expect fun Float.toPercentage(point: Int = 2): String