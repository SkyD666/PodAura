package com.skyd.podaura.ext

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize

fun ImageBitmap.size(): IntSize {
    return IntSize(width = width, height = height)
}