package io.github.alexzhirkevich.compottie.internal.animation

import io.github.alexzhirkevich.compottie.internal.helpers.BooleanIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public class VectorKeyframe(

    @SerialName("s")
    override val start: FloatArray? = null,

    @SerialName("e")
    override val end: FloatArray? = null,

    @SerialName("t")
    override val time: Float,

    @SerialName("h")
    @Serializable(with = BooleanIntSerializer::class)
    override val hold: Boolean = false,

    @SerialName("i")
    override val inValue: BezierInterpolation? = null,

    @SerialName("o")
    override val outValue: BezierInterpolation? = null,

    @SerialName("ti")
    public val inTangent: FloatArray? = null,

    @SerialName("to")
    public val outTangent: FloatArray? = null,
) : Keyframe<FloatArray> by BaseKeyframe(
    start = start,
    end = end,
    time = time,
    hold = hold,
    inValue = inValue,
    outValue = outValue
) {
    internal fun copy(): VectorKeyframe {
        return VectorKeyframe(
            start = start,
            end = end,
            time = time,
            hold = hold,
            inValue = inValue,
            outValue = outValue,
            inTangent = inTangent,
            outTangent = outTangent
        )
    }
}
