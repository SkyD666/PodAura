package io.github.alexzhirkevich.compottie.internal.effects

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Paint
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.animation.RawProperty
import io.github.alexzhirkevich.compottie.internal.helpers.BooleanIntSerializer
import io.github.alexzhirkevich.compottie.internal.utils.getAs
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("21")
internal class FillEffect(
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

    val color get() = values.getAs<EffectValue.Color>(2)?.value

    val opacity get() = values.getAs<EffectValue.Slider>(6)?.value

    override fun apply(
        paint: Paint,
        animationState: AnimationState,
        effectState: LayerEffectsState
    ) {
        val color = color?.interpolatedColor(animationState)
            ?.let(::Color)
            ?.let {
                it.copy(                              // don't divide by 100
                    alpha = it.alpha * (opacity?.interpolatedFloat(animationState)?.coerceIn(0f, 1f) ?: 1f)
                )
            }
        if (paint !== effectState.lastPaint || effectState.lastFillColor != color) {

            paint.colorFilter = color?.let {
                ColorFilter.tint(color)
            }
            effectState.lastFillFilter = paint.colorFilter
            effectState.lastFillColor = color
        } else {
            paint.colorFilter = effectState.lastFillFilter
        }
    }

    override fun copy(): LayerEffect {
        return FillEffect(
            values = values.map(EffectValue<RawProperty<Any>>::copy),
            name = name,
            matchName = matchName,
            index = index,
            enabled = enabled
        )
    }
}