package io.github.alexzhirkevich.compottie.internal.helpers

import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedGradient
import io.github.alexzhirkevich.compottie.internal.animation.ExpressionHolder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class GradientColors(

    @SerialName("k")
    val colors: AnimatedGradient,

    @SerialName("p")
    val numberOfColors: Int = 0,

    val sid: String? = null
) : ExpressionHolder {

    fun interpolated(state: AnimationState): ColorsWithStops {
        colors.numberOfColors = numberOfColors

        return if (sid != null) {
            state.composition.slotResolver.gradient(sid, state)
                ?.also {
                    if (it.numberOfColors == 0) {
                        it.numberOfColors = numberOfColors
                    }
                }
                ?.interpolated(state)
                ?: colors.interpolated(state)
        } else {
            colors.interpolated(state)
        }
    }

    override fun prepareExpressions(state: AnimationState) {
        colors.prepareExpressions(state)
    }

    fun copy() = GradientColors(
        colors = colors.copy(),
        numberOfColors = numberOfColors,
        sid = sid
    )
}