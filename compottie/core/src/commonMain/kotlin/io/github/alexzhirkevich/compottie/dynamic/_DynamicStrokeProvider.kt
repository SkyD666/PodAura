package io.github.alexzhirkevich.compottie.dynamic

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedNumber
import io.github.alexzhirkevich.compottie.internal.platform.GradientCache

internal class DynamicStrokeProvider : BaseDynamicDrawProvider(), DynamicStroke {

    var width: PropertyProvider<Float>? = null
        private set

    override fun width(provider: PropertyProvider<Float>) {
        this.width = provider
    }
}

internal fun DynamicStrokeProvider?.applyToPaint(
    paint: Paint,
    state: AnimationState,
    parentAlpha: Float,
    parentMatrix: Matrix,
    opacity: AnimatedNumber?,
    strokeWidth: AnimatedNumber,
    size: () -> Rect,
    gradientCache: GradientCache
) {
    applyToPaint(
        paint = paint,
        state = state,
        parentAlpha = parentAlpha,
        parentMatrix = parentMatrix,
        opacity = opacity,
        size = size,
        gradientCache = gradientCache
    )

    paint.strokeWidth = strokeWidth.interpolated(state)
    paint.strokeWidth = this?.width.derive(paint.strokeWidth, state)
//    paint.strokeWidth *= parentMatrix.scale
}

