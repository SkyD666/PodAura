package io.github.alexzhirkevich.compottie.internal.effects

import androidx.compose.ui.graphics.Paint
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.animation.RawProperty
import io.github.alexzhirkevich.compottie.internal.helpers.BooleanIntSerializer
import io.github.alexzhirkevich.compottie.internal.platform.setBlurMaskFilter
import io.github.alexzhirkevich.compottie.internal.utils.getAs
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("29")
internal class BlurEffect(
    @SerialName("ef")
    override val values: List<EffectValue<@Contextual RawProperty<@Contextual Any>>>,

    @SerialName("nm")
    override val name: String? = null,

    @SerialName("mn")
    override val matchName: String? = null,

    @SerialName("ix")
    override val index: Int? = null,

    @SerialName("en")
    @Serializable(with = BooleanIntSerializer::class)
    override val enabled: Boolean = true,
) : LayerEffect() {

    val radius get() = values.getAs<EffectValue.Slider>(0)?.value

    override fun apply(
        paint: Paint,
        animationState: AnimationState,
        effectState: LayerEffectsState
    ) {
        val radius = radius?.interpolatedFloat(animationState)
        if (radius == null || radius <= 0f)
            return

        if (paint !== effectState.lastPaint || radius != effectState.blurRadius) {
            paint.setBlurMaskFilter(radius)
            effectState.blurRadius = radius
        }
    }

    override fun copy(): LayerEffect {
        return BlurEffect(
            values = values.map(EffectValue<RawProperty<Any>>::copy),
            name = name,
            matchName = matchName,
            index = index,
            enabled = enabled
        )
    }

    override fun prepareExpressions(state: AnimationState) {

    }
}