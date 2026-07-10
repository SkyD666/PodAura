package io.github.alexzhirkevich.compottie.internal.platform

import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Paint

internal fun Canvas.drawRect(rect: MutableRect, paint: Paint) =
    drawRect(
        left = rect.left,
        top = rect.top,
        right = rect.right,
        bottom = rect.bottom,
        paint = paint
    )

internal fun Canvas.clipRect(rect: MutableRect) {
    clipRect(
        left = rect.left,
        top = rect.top,
        right = rect.right,
        bottom = rect.bottom,
    )
}


internal expect fun Canvas.saveLayer(rect: MutableRect, paint: Paint, flag: Int)

internal fun Canvas.saveLayer(rect: MutableRect, paint: Paint) = saveLayer(rect.toRect(), paint)
