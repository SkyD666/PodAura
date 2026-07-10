package io.github.alexzhirkevich.compottie.internal.animation

import androidx.compose.ui.graphics.Matrix
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.utils.fastReset
import io.github.alexzhirkevich.compottie.internal.utils.preRotate
import io.github.alexzhirkevich.compottie.internal.utils.preScale
import io.github.alexzhirkevich.compottie.internal.utils.preTranslate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.pow

@Serializable
internal class RepeaterTransform(
    @SerialName("a")
    override val anchorPoint: AnimatedVector2 = AnimatedVector2.defaultAnchorPoint(),

    @SerialName("p")
    override val position: AnimatedVector2 = AnimatedVector2.defaultPosition(),

    @SerialName("s")
    override val scale: AnimatedVector2 = AnimatedVector2.defaultScale(),

    @SerialName("r")
    override val rotation: AnimatedNumber = AnimatedNumber.defaultRotation(),

    @SerialName("o")
    override val opacity: AnimatedNumber = AnimatedNumber.defaultOpacity(),

    @SerialName("sk")
    override val skew: AnimatedNumber = AnimatedNumber.defaultSkew(),

    @SerialName("sa")
    override val skewAxis: AnimatedNumber = AnimatedNumber.defaultSkewAxis(),

    @SerialName("so")
    val startOpacity: AnimatedNumber? = null,

    @SerialName("eo")
    val endOpacity: AnimatedNumber? = null,
) : AnimatedTransform() {

    fun repeaterMatrix(state: AnimationState, amount: Float): Matrix {
        matrix.fastReset()

        val pos = Vec2(position.interpolatedVec(state))
        matrix.preTranslate(
            pos.x * amount,
            pos.y * amount
        )

        val sc = Vec2(scale.interpolatedNorm(state))
        matrix.preScale(
            sc.x.pow(amount),
            sc.y.pow(amount)
        )

        rotation.interpolatedFloat(state).let {
            val anchorPoint = Vec2(anchorPoint.interpolatedVec(state))
            matrix.translate(anchorPoint.x, anchorPoint.y)
            matrix.preRotate(it * amount)
            matrix.translate(-anchorPoint.x, -anchorPoint.y)
        }

        return matrix
    }

    override fun prepareExpressions(state: AnimationState) {
        super.prepareExpressions(state)
        startOpacity?.prepareExpressions(state)
        endOpacity?.prepareExpressions(state)
    }

    fun deepCopy() = RepeaterTransform(
        anchorPoint = anchorPoint.copy(),
        position = position.copy(),
        scale = scale.copy(),
        rotation = rotation.copy(),
        opacity = opacity.copy(),
        skew = skew.copy(),
        skewAxis = skewAxis.copy(),
        startOpacity = startOpacity?.copy(),
        endOpacity = endOpacity?.copy()
    )
}