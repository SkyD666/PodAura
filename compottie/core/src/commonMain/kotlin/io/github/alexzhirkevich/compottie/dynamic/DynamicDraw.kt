package io.github.alexzhirkevich.compottie.dynamic

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.util.fastMap
import io.github.alexzhirkevich.compottie.internal.AnimationState

public sealed interface DynamicDraw : DynamicShape {

    public fun opacity(provider: PropertyProvider<Float>)

    public fun colorFilter(provider: PropertyProvider<ColorFilter?>)

    public fun blendMode(provider: PropertyProvider<BlendMode>)

    public fun color(provider: PropertyProvider<Color>)

    /**
     * Dynamic gradient provider.
     *
     * Will be invoked for each frame therefore gradient instance should be cached if possible
     *
     * Example:
     *
     * ```kotlin
     * gradient { bounds ->
     *     LottieGradient.Linear(
     *          colorStops = listOf(0f to Color.Red, 1f to Color.Blue),
     *          start = bounds.topLeft,
     *          end = bounds.bottomRight
     *      )
     * }
     * ```
     * */
    public fun gradient(provider: GradientProvider)
}

public typealias GradientProvider = AnimationState.(Rect) -> LottieGradient

public sealed interface LottieGradient {

    /**
     * Linear gradient. Similar to [androidx.compose.ui.graphics.Brush.linearGradient]
     *
     * @param colorStops colors and offsets to determine how the colors are dispersed throughout the
     * radial gradient
     * @param start starting position of the linear gradient. This can be set to [Offset.Zero] to
     * position at the far left and top of the drawing area
     * @param end - ending position of the linear gradient. This can be set to [Offset.Infinite] to
     * position at the far right and bottom of the drawing area
     * */
    public data class Linear(
        val colorStops: List<Pair<Float, Color>>,
        val start: Offset = Offset.Zero,
        val end: Offset = Offset.Infinite,
    ) : LottieGradient {
        internal val colors = colorStops.fastMap { it.second }
        internal val stops = colorStops.fastMap { it.first }
    }

    /**
     * Radial gradient. Similar to [androidx.compose.ui.graphics.Brush.radialGradient]
     *
     * @param colorStops colors and offsets to determine how the colors are dispersed throughout the
     * radial gradient
     * @param center center position of the radial gradient circle. If this is set to
     * [Offset.Unspecified] then the center of the drawing area is used as the center for the
     * radial gradient. [Float.POSITIVE_INFINITY] can be used for either [Offset.x] or [Offset.y] to
     * indicate the far right or far bottom of the drawing area respectively.
     * @param radius radius for the radial gradient. Defaults to positive infinity to indicate
     * the largest radius that can fit within the bounds of the drawing area
     * @param highlightingAngle the angle in degrees the point is offset from the line between the center and center right point
     * @param highlightingLength the percentage of the radius (between 0 and 1) indicating the distance from s
     * */
    public data class Radial(
        val colorStops: List<Pair<Float, Color>>,
        val center: Offset = Offset.Unspecified,
        val radius: Float = Float.POSITIVE_INFINITY,
        val highlightingAngle: Float = 0f,
        val highlightingLength: Float = 0f,
    ) : LottieGradient {
        internal val colors = colorStops.fastMap { it.second }
        internal val stops = colorStops.fastMap { it.first }
    }

    /**
     * Conic (sweep) gradient. Similar to [androidx.compose.ui.graphics.Brush.sweepGradient]
     *
     * @param colorStops colors and offsets to determine how the colors are dispersed throughout the
     * radial gradient
     * @param center center position of the radial gradient circle. If this is set to
     * [Offset.Unspecified] then the center of the drawing area is used as the center for the
     * radial gradient. [Float.POSITIVE_INFINITY] can be used for either [Offset.x] or [Offset.y] to
     * indicate the far right or far bottom of the drawing area respectively.
     * @param angle the initial angle in degrees
     * */
    public data class Conic(
        val colorStops: List<Pair<Float, Color>>,
        val center: Offset = Offset.Unspecified,
        val angle: Float = 0f,
    ) : LottieGradient {
        internal val colors = colorStops.fastMap { it.second }
        internal val stops = colorStops.fastMap { it.first }
    }
}
