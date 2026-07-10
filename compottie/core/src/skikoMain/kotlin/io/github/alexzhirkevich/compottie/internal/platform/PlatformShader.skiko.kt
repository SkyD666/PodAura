package io.github.alexzhirkevich.compottie.internal.platform

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asComposeShader
import androidx.compose.ui.graphics.skiaPaint
import androidx.compose.ui.graphics.toArgb
import io.github.alexzhirkevich.compottie.internal.utils.degreeToRadians
import org.jetbrains.skia.Color4f
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Gradient
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.MaskFilter
import org.jetbrains.skia.Matrix33
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import org.jetbrains.skia.Shader as SkShader

internal actual fun MakeLinearGradient(
    from: Offset,
    to: Offset,
    colors: List<Color>,
    colorStops: List<Float>,
    tileMode: TileMode,
    matrix: Matrix
): Shader = SkShader.makeLinearGradient(
    x0 = from.x,
    y0 = from.y,
    x1 = to.x,
    y1 = to.y,
    gradient = Gradient(
        colors = Gradient.Colors(
            colors = colors.toColor4fArray(),
            positions = colorStops.toFloatArray(),
            tileMode = FilterTileMode.CLAMP
        ),
        interpolation = Gradient.Interpolation(
            inPremul = Gradient.Interpolation.InPremul.YES
        )
    ),
    localMatrix = matrix.asSkia33(coerceScale = true)
).asComposeShader()

private val tmpMatrix = Matrix()

internal actual fun MakeRadialGradient(
    center: Offset,
    radius: Float,
    highlightingAngle: Float,
    highlightingLength: Float,
    colors: List<Color>,
    colorStops: List<Float>,
    tileMode: TileMode,
    matrix: Matrix
) = SkShader.makeRadialGradient(
    x = center.x,
    y = center.y,
    radius = radius,
    gradient = Gradient(
        colors = Gradient.Colors(
            colors = colors.toColor4fArray(),
            positions = colorStops.toFloatArray(),
            tileMode = tileMode.toSkiaTileMode()
        ),
        interpolation = Gradient.Interpolation(
            inPremul = Gradient.Interpolation.InPremul.YES
        )
    ),
    localMatrix = if (highlightingLength == 0f) {
        matrix.asSkia33(coerceScale = true)
    } else {
        val angle = degreeToRadians(highlightingAngle)
        val focalOffsetX = highlightingLength * sin(angle)
        val focalOffsetY = highlightingLength * cos(angle)

        tmpMatrix.resetToPivotedTransform(
            pivotX = center.x,
            pivotY = center.y,
            translationX = focalOffsetX,
            translationY = focalOffsetY,
        )
        tmpMatrix.timesAssign(matrix)
        tmpMatrix.asSkia33(coerceScale = true)
    }
).asComposeShader()

internal actual fun MakeSweepGradient(
    center: Offset,
    angle: Float,
    colors: List<Color>,
    colorStops: List<Float>,
    matrix: Matrix
): Shader = SkShader.makeSweepGradient(
    x = center.x,
    y = center.y,
    gradient = Gradient(
        colors = Gradient.Colors(
            colors = colors.toColor4fArray(),
            positions = colorStops.toFloatArray(),
            tileMode = TileMode.Clamp.toSkiaTileMode()
        ),
        interpolation = Gradient.Interpolation(
            inPremul = Gradient.Interpolation.InPremul.YES
        )
    ),
    localMatrix = matrix
        .asSkia33(coerceScale = true)
        .let {
            if (angle == 0f) {
                it
            } else {
                Matrix33.makeRotate(angle, center.x, center.y)
            }
        }
).asComposeShader()


private val tmpMatrix33 = Matrix33.makeTranslate(0f, 0f)

internal fun Matrix.asSkia33(coerceScale: Boolean = false): Matrix33 {

    // skiko shaders with zero scale cause crash

    val scaleX = when {
        coerceScale && abs(values[Matrix.ScaleX]) < 0.001f -> 0.001f
        else -> values[Matrix.ScaleX]
    }

    val scaleY = when {
        coerceScale && abs(values[Matrix.ScaleY]) < 0.001f -> 0.001f
        else -> values[Matrix.ScaleY]
    }

    return tmpMatrix33.apply {
        mat[0] = scaleX
        mat[1] = values[Matrix.SkewX]
        mat[2] = values[Matrix.TranslateX]
        mat[3] = values[Matrix.SkewY]
        mat[4] = scaleY
        mat[5] = values[Matrix.TranslateY]
        mat[6] = values[Matrix.Perspective0]
        mat[7] = values[Matrix.Perspective1]
        mat[8] = values[Matrix.Perspective2]
    }
}

private fun List<Color>.toIntArray(): IntArray =
    IntArray(size) { i -> this[i].toArgb() }

private fun List<Color>.toColor4fArray(): Array<Color4f> =
    Array(size) { i ->
        val color = this[i]
        Color4f(color.red, color.green, color.blue, color.alpha)
    }

internal fun TileMode.toSkiaTileMode(): FilterTileMode = when (this) {
    TileMode.Clamp -> FilterTileMode.CLAMP
    TileMode.Repeated -> FilterTileMode.REPEAT
    TileMode.Mirror -> FilterTileMode.MIRROR
    TileMode.Decal -> FilterTileMode.DECAL
    else -> FilterTileMode.CLAMP
}

// internal actual val ColorFilter.Companion.Luma get() = org.jetbrains.skia.ColorFilter.luma.asComposeColorFilter()


internal actual fun Paint.setBlurMaskFilter(radius: Float, isImage: Boolean) {
    val skPaint = skiaPaint

    val sigma = if (radius > 0) {
        BlurSigmaScale * radius
    } else {
        0.0f
    }

    if (sigma > 0f) {
        if (isImage) {
            skPaint.imageFilter = ImageFilter.makeBlur(sigma, sigma, FilterTileMode.DECAL)
        } else {
            skPaint.maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, sigma)
        }
    } else {
        skPaint.imageFilter = null
        skPaint.maskFilter = null
    }
}

internal const val BlurSigmaScale = .3f
