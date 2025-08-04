package com.skyd.podaura.ext

fun Float.toDegrees(): Float = (this * 180 / Math.PI).toFloat()

fun Int.toSignedString(): String = if (this >= 0) "+$this" else toString()