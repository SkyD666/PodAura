package com.skyd.podaura.ext

import androidx.compose.ui.geometry.Size

val Size.ratio: Float?
    get() {
        return runCatching { width / height }.getOrNull()?.takeIf { !it.isNaN() }
    }