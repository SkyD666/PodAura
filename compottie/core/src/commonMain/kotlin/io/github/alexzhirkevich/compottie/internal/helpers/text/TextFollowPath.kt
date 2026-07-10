package io.github.alexzhirkevich.compottie.internal.helpers.text

import io.github.alexzhirkevich.compottie.internal.animation.AnimatedNumber
import io.github.alexzhirkevich.compottie.internal.helpers.Mask
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class TextFollowPath(

    @SerialName("m")
    val mask: Mask? = null,

    @SerialName("f")
    val firstMargin: AnimatedNumber? = null,

    @SerialName("l")
    val lastMargin: AnimatedNumber? = null,

    @SerialName("r")
    val reversePath: AnimatedNumber? = null,

    @SerialName("a")
    val forceAlignment: AnimatedNumber? = null,

    @SerialName("p")
    val perpendicularToPath: AnimatedNumber? = null,
) {
    fun deepCopy() = TextFollowPath(
        mask = mask?.deepCopy(),
        firstMargin = firstMargin?.copy(),
        lastMargin = lastMargin?.copy(),
        reversePath = reversePath?.copy(),
        forceAlignment = forceAlignment?.copy(),
        perpendicularToPath = perpendicularToPath?.copy()
    )
}