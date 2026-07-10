package io.github.alexzhirkevich.compottie.internal.animation

import io.github.alexzhirkevich.compottie.internal.helpers.text.TextDocument
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class TextDocumentKeyframe(
    @SerialName("s")
    override val start: TextDocument? = null,

    @SerialName("e")
    override val end: TextDocument? = null,

    @SerialName("t")
    override val time: Float,

    //not used
    override val hold: Boolean = false,
    override val inValue: BezierInterpolation? = null,
    override val outValue: BezierInterpolation? = null
) : Keyframe<TextDocument> by BaseKeyframe(
    start = start,
    end = end,
    time = time,
    hold = hold,
    inValue = inValue,
    outValue = outValue
)