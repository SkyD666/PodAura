package io.github.alexzhirkevich.compottie.internal.animation

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import kotlin.math.abs
import kotlin.math.absoluteValue

public interface Keyframe<out T> {
    public val start: T?
    public val end: T?
    public val time: Float
    public val hold: Boolean
    public val inValue: BezierInterpolation?
    public val outValue: BezierInterpolation?

    public val endHold: T? get() = if (hold) start else end

    public val easingX: Easing
    public val easingY: Easing
}

internal class BaseKeyframe<out T>(
    override val start: T?,
    override val end: T?,
    override val time: Float,
    override val hold: Boolean,
    override val inValue: BezierInterpolation?,
    override val outValue: BezierInterpolation?,
) : Keyframe<T> {

    override val easingX: Easing = run {
        val i = inValue
        val o = outValue

        if (hold || i == null || o == null) {
            LinearEasing
        } else {
            if (i.x.isNotEmpty() &&
                i.y.isNotEmpty() &&
                o.x.isNotEmpty() &&
                o.y.isNotEmpty()
            ) {
                PreciseCubicBezier(
                    o.x[0].clampX(),
                    o.y[0].clampY(),
                    i.x[0].clampX(),
                    i.y[0].clampY()
                )
            } else LinearEasing
        }
    }

    override val easingY = run {

        val i = inValue
        val o = outValue

        if (hold || i == null || o == null) {
            return@run LinearEasing
        }

        if (i.x.size < 2 || i.y.size < 2 || o.x.size < 2 || o.y.size == 2) {
            return@run easingX
        }

        PreciseCubicBezier(
            o.x[1].clampX(),
            o.y[1].clampY(),
            i.x[1].clampX(),
            i.y[1].clampY()
        )
    }
}

private fun Float.clampX() = coerceIn(-1f, 1f)
private fun Float.clampY() = coerceIn(-100f, 100f)

private class PreciseCubicBezier(
    private val a: Float,
    private val b: Float,
    private val c: Float,
    private val d: Float
) : Easing {

    private val isLinear = (abs(a - b) < CubicErrorBound && abs(c - d) < CubicErrorBound)

    private fun evaluateCubic(a: Float, b: Float, m: Float): Float {
        return 3 * a * (1 - m) * (1 - m) * m +
                3 * b * (1 - m) * /*    */ m * m +
                /*                      */ m * m * m
    }

    override fun transform(fraction: Float): Float {

        if (isLinear)
            return fraction

        if (fraction > 0f && fraction < 1f) {
            var start = 0.0f
            var end = 1.0f
            while (true) {
                val midpoint = (start + end) / 2
                val estimate = evaluateCubic(a, c, midpoint)
                if ((fraction - estimate).absoluteValue < CubicErrorBound)
                    return evaluateCubic(b, d, midpoint)
                if (estimate < fraction)
                    start = midpoint
                else
                    end = midpoint
            }
        } else {
            return fraction
        }
    }
}

private const val CubicErrorBound: Float = 0.0001f


