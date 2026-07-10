package io.github.alexzhirkevich.compottie.internal.animation

import androidx.compose.ui.graphics.Color

internal class ColorKeyframe(
    override val start: Color?,
    override val end: Color?,
    time: Float,
    hold: Boolean = false,
    inValue: BezierInterpolation? = null,
    outValue: BezierInterpolation? = null,
) : Keyframe<Color> by BaseKeyframe(
    start = start,
    end = end,
    time = time,
    hold = hold,
    inValue = inValue,
    outValue = outValue
) {
    fun copy(): ColorKeyframe {
        return ColorKeyframe(
            start = start,
            end = end,
            time = time,
            hold = hold,
            inValue = inValue,
            outValue = outValue
        )
    }
}