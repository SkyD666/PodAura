@file:Suppress("UNCHECKED_CAST")

package io.github.alexzhirkevich.compottie.internal.animation

import androidx.compose.ui.util.fastMap
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.helpers.ColorsWithStops
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.Undefined
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

@Serializable(with = AnimatedGradientSerializer::class)
public sealed class AnimatedGradient : ExpressionProperty<ColorsWithStops>() {

    @Transient
    public var numberOfColors: Int = 0

    private val tempExpressionColors by lazy {
        ColorsWithStops(numberOfColors)
    }

    internal abstract fun copy(): AnimatedGradient

    override fun mapEvaluated(e: Any): ColorsWithStops {
        return when (e) {
            is ColorsWithStops -> e
            is FloatArray -> tempExpressionColors.apply {
                fill(e, numberOfColors)
            }

            is List<*> -> tempExpressionColors.apply {
                fill(
                    (e as List<Number>).fastMap(Number::toFloat).toFloatArray(),
                    numberOfColors
                )
            }

            else -> error("Failed to cast $e to gradient vector")
        }
    }

    @Serializable
    public class Default(
        @SerialName("k")
        public val colorsVector: FloatArray,

        @SerialName("ix")
        override val index: Int? = null,

        @SerialName("x")
        override val expression: String? = null
    ) : AnimatedGradient() {

        private val tempColors by lazy {
            ColorsWithStops(numberOfColors).apply {
                fill(colorsVector, numberOfColors)
            }
        }

        override fun raw(state: AnimationState): ColorsWithStops {
            return tempColors
        }

        override fun copy(): AnimatedGradient {
            return Default(
                colorsVector = colorsVector,
                index = index,
                expression = expression
            )
        }
    }

    @Serializable
    public class Animated(
        @SerialName("k")
        override val keyframes: List<VectorKeyframe>,
        @SerialName("ix")
        override val index: Int? = null,
        @SerialName("x")
        override val expression: String? = null
    ) : AnimatedGradient(), AnimatedKeyframeProperty<ColorsWithStops, VectorKeyframe> {

        private val tempColors by lazy {
            ColorsWithStops(numberOfColors)
        }

        private val tempColorsA by lazy {
            ColorsWithStops(numberOfColors)
        }

        private val tempColorsB by lazy {
            ColorsWithStops(numberOfColors)
        }

        @Transient
        private val delegate = BaseKeyframeAnimation(
            index = index,
            sourceKeyframes = keyframes,
            emptyValue = tempColors
        ) { s, e, p ->
            val progress = easingX.transform(p)

            tempColorsA.fill(s, numberOfColors)
            tempColorsB.fill(e, numberOfColors)

            tempColors.apply {
                interpolateBetween(tempColorsA, tempColorsB, progress)
            }
        }

        override fun copy(): AnimatedGradient {
            return Animated(
                keyframes = keyframes,
                index = index,
                expression = expression
            )
        }

        override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
            return super<AnimatedGradient>.get(property, runtime).let {
                if (it is Undefined)
                    super<AnimatedKeyframeProperty>.get(property, runtime)
                else it
            }
        }

        override fun raw(state: AnimationState): ColorsWithStops {
            return delegate.raw(state)
        }
    }
}

internal object AnimatedGradientSerializer : JsonContentPolymorphicSerializer<AnimatedGradient>(AnimatedGradient::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<AnimatedGradient> {
        check(element is JsonObject) {
            "Invalid gradient: $element"
        }

        return when {
            element["a"]?.jsonPrimitive?.int == 1 -> AnimatedGradient.Animated.serializer()
            else -> AnimatedGradient.Default.serializer()
        }
    }

}
