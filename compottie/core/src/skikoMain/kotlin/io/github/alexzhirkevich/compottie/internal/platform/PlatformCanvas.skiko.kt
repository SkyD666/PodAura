package io.github.alexzhirkevich.compottie.internal.platform

import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.graphics.skiaPaint

internal actual fun Canvas.saveLayer(rect: MutableRect, paint: Paint, flag: Int) {
    try {
        skiaCanvas.saveLayer(
            left = rect.left,
            top = rect.top,
            right = rect.right,
            bottom = rect.bottom,
            paint = paint.skiaPaint
        )
    } catch (_: ClassCastException) {
    }
}
