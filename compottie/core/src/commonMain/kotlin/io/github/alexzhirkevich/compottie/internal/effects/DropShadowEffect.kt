package io.github.alexzhirkevich.compottie.internal.effects

import androidx.compose.ui.graphics.Paint
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.animation.RawProperty
import io.github.alexzhirkevich.compottie.internal.helpers.BooleanIntSerializer
import io.github.alexzhirkevich.compottie.internal.utils.getAs
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("25")
internal class DropShadowEffect(
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

    val color get() = values.getAs<EffectValue.Color>(0)?.value
    val opacity get() = values.getAs<EffectValue.Slider>(1)?.value
    val angle get() = values.getAs<EffectValue.Angle>(2)?.value
    val distance get() = values.getAs<EffectValue.Slider>(3)?.value
    val blur get() = values.getAs<EffectValue.Slider>(4)?.value

    override fun apply(
        paint: Paint,
        animationState: AnimationState,
        effectState: LayerEffectsState
    ) {
        // TODO drop shadow effect
    }

    override fun copy(): LayerEffect {
        return DropShadowEffect(
            values = values.map(EffectValue<RawProperty<Any>>::copy),
            name = name,
            matchName = matchName,
            index = index,
            enabled = enabled
        )
    }
}