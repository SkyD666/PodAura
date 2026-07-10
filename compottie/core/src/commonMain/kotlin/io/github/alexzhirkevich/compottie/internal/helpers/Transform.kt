package io.github.alexzhirkevich.compottie.internal.helpers

import io.github.alexzhirkevich.compottie.internal.animation.AnimatedNumber
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedTransform
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedVector2
import io.github.alexzhirkevich.compottie.internal.animation.ExpressionHolder
import io.github.alexzhirkevich.compottie.internal.animation.PropertyGroup
import io.github.alexzhirkevich.compottie.internal.animation.defaultAnchorPoint
import io.github.alexzhirkevich.compottie.internal.animation.defaultOpacity
import io.github.alexzhirkevich.compottie.internal.animation.defaultPosition
import io.github.alexzhirkevich.compottie.internal.animation.defaultRotation
import io.github.alexzhirkevich.compottie.internal.animation.defaultScale
import io.github.alexzhirkevich.compottie.internal.animation.defaultSkew
import io.github.alexzhirkevich.compottie.internal.animation.defaultSkewAxis
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class Transform(

    @SerialName("a")
    override val anchorPoint: AnimatedVector2 = AnimatedVector2.defaultAnchorPoint(),

    @SerialName("p")
    override val position: AnimatedVector2 = AnimatedVector2.defaultPosition(),

    @SerialName("s")
    override val scale: AnimatedVector2 = AnimatedVector2.defaultScale(),

    @SerialName("r")
    override val rotation: AnimatedNumber = AnimatedNumber.defaultRotation(),

    @SerialName("rx")
    override val rotationX: AnimatedNumber? = null,

    @SerialName("ry")
    override val rotationY: AnimatedNumber? = null,

    @SerialName("rz")
    override val rotationZ: AnimatedNumber? = null,

    @SerialName("o")
    override val opacity: AnimatedNumber = AnimatedNumber.defaultOpacity(),

    @SerialName("sk")
    override val skew: AnimatedNumber = AnimatedNumber.defaultSkew(),

    @SerialName("sa")
    override val skewAxis: AnimatedNumber = AnimatedNumber.defaultSkewAxis(),
) : AnimatedTransform(), ExpressionHolder, PropertyGroup {

    fun deepCopy(): Transform {
        return Transform(
            anchorPoint = anchorPoint.copy(),
            position = position.copy(),
            scale = scale.copy(),
            rotation = rotation.copy(),
            rotationX = rotationX?.copy(),
            rotationY = rotationY?.copy(),
            rotationZ = rotationZ?.copy(),
            opacity = opacity.copy(),
            skew = skew.copy(),
            skewAxis = skewAxis.copy()
        )
    }
}

