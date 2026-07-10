package io.github.alexzhirkevich.compottie.internal.effects

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Paint
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.animation.RawProperty
import io.github.alexzhirkevich.compottie.internal.animation.interpolatedNorm
import io.github.alexzhirkevich.compottie.internal.animation.toColorLong
import io.github.alexzhirkevich.compottie.internal.helpers.BooleanIntSerializer
import io.github.alexzhirkevich.compottie.internal.utils.getAs
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("20")
internal class TintEffect(
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

    val black
        get() = values.getAs<EffectValue.Color>(0)?.value

    val white
        get() = values.getAs<EffectValue.Color>(1)?.value

    val intensity
        get() = values.getAs<EffectValue.Slider>(2)?.value

    override fun apply(
        paint: Paint,
        animationState: AnimationState,
        effectState: LayerEffectsState
    ) {
        val intensity = (intensity?.interpolatedNorm(animationState) ?: 1f).coerceIn(0f, 1f)

        val black = Color(black?.interpolatedColor(animationState) ?: Color.Black.toColorLong())
            .let { it.copy(alpha = it.alpha * intensity) }

        val white = white?.interpolatedColor(animationState)
            ?.let(::Color)
            ?.let { it.copy(alpha = it.alpha * intensity) }

        if (black.red != 0f || black.green != 0f || black.blue != 0f)
            return //unsupported

        val hash = white.hashCode()

        if (paint === effectState.lastPaint &&
            hash == effectState.tintHash &&
            effectState.tintColorFiter != null
        ) {
            paint.colorFilter = effectState.tintColorFiter
            return
        }
        paint.colorFilter = if (white != null) {
            ColorFilter.tint(white, BlendMode.Modulate)
        } else null

        effectState.tintHash = hash
        effectState.tintColorFiter = paint.colorFilter
    }

    override fun copy(): LayerEffect {
        return TintEffect(
            values = values.map { it.copy() },
            name = name,
            matchName = matchName,
            index = index,
            enabled = enabled
        )
    }
}