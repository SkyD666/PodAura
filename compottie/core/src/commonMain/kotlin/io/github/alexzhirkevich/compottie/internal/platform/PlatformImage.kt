package io.github.alexzhirkevich.compottie.internal.platform

import androidx.compose.ui.graphics.ImageBitmap

internal expect suspend fun ImageBitmap.Companion.fromBytes(
    bytes: ByteArray,
    width: Int,
    height: Int
): ImageBitmap