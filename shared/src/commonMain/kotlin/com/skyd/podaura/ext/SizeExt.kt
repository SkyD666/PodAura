package com.skyd.podaura.ext

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize

val Size.ratio: Float?
    get() {
        return runCatching { width / height }.getOrNull()?.takeIf { !it.isNaN() }
    }

val IntSize.ratio: Float?
    get() {
        return runCatching { width * 1f / height }.getOrNull()?.takeIf { !it.isNaN() }
    }