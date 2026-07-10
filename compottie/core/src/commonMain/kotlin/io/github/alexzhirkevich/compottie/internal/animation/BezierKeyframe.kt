package io.github.alexzhirkevich.compottie.internal.animation

import io.github.alexzhirkevich.compottie.internal.helpers.Bezier
import io.github.alexzhirkevich.compottie.internal.helpers.BooleanIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonTransformingSerializer

@Serializable
public class BezierKeyframe internal constructor(

    @SerialName("s")
    @Serializable(with = BezierSerializer::class)
    override val start: Bezier? = null,

    @SerialName("e")
    @Serializable(with = BezierSerializer::class)
    override val end: Bezier? = null,

    @SerialName("t")
    override val time: Float,

    @SerialName("h")
    @Serializable(with = BooleanIntSerializer::class)
    override val hold: Boolean = false,

    @SerialName("i")
    override val inValue: BezierInterpolation? = null,

    @SerialName("o")
    override val outValue: BezierInterpolation? = null,
) : Keyframe<Bezier> by BaseKeyframe(
    start = start,
    end = end,
    time = time,
    hold = hold,
    inValue = inValue,
    outValue = outValue
)

internal class BezierSerializer : JsonTransformingSerializer<Bezier>(Bezier.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        if (element is JsonArray) {
            return element.first()
        }
        return element
    }
}