@file:Suppress("UNCHECKED_CAST")

package io.github.alexzhirkevich.compottie.internal.animation

import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.lerp
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.Undefined
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.min


@Serializable(with = AnimatedVectorNSerializer::class)
internal sealed class AnimatedVectorN : DynamicProperty<List<Float>>() {

    override fun mapEvaluated(e: Any): List<Float> {
        return when (e) {
            is List<*> -> (e as List<Number>).fastMap { it.toFloat() }
            else -> error("Failed to cast $e to Vec2")
        }
    }

    abstract fun copy(): AnimatedVectorN


    @Serializable
    class Default(
        @SerialName("k")
        @Serializable(with = ToListSerializer::class)
        val value: List<Float>,

        @SerialName("x")
        override val expression: String? = null,

        @SerialName("ix")
        override val index: Int? = null
    ) : AnimatedVectorN() {

        override fun raw(state: AnimationState): List<Float> = value

        override fun copy(): AnimatedVectorN {
            return Default(
                value = value,
                expression = expression,
                index = index
            )
        }
    }

    @Serializable
    class Animated(
        @SerialName("k")
        override val keyframes: List<VectorKeyframe>,

        @SerialName("x")
        override val expression: String? = null,

        @SerialName("ix")
        override val index: Int? = null
    ) : AnimatedVectorN(), AnimatedKeyframeProperty<List<Float>, VectorKeyframe> {

        @Transient
        private val delegate = BaseKeyframeAnimation(
            index = index,
            sourceKeyframes = keyframes,
            emptyValue = emptyList(),
            map = { s, e, p ->
                val p = easingX.transform(p)

                List(min(s.size, e.size)) {
                    lerp(s[it], e[it], p)
                }
            }
        )

        override fun raw(state: AnimationState): List<Float> {
            return delegate.raw(state)
        }

        override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
            return super<AnimatedVectorN>.get(property, runtime).let {
                if (it is Undefined)
                    super<AnimatedKeyframeProperty>.get(property, runtime)
                else it
            }
        }

        override fun copy(): AnimatedVectorN {
            return Animated(
                keyframes = keyframes,
                expression = expression,
                index = index
            )
        }
    }
}


internal class AnimatedVectorNSerializer : JsonContentPolymorphicSerializer<AnimatedVectorN>(AnimatedVectorN::class) {

    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<AnimatedVectorN> {

        val k = element.jsonObject["k"]

        return when {

            element.jsonObject["a"]?.jsonPrimitive?.intOrNull == 1 ||
                    k is JsonArray && k[0] is JsonObject ->
                AnimatedVectorN.Animated.serializer()

            else -> AnimatedVectorN.Default.serializer()
        }
    }
}

internal class ToListSerializer : JsonTransformingSerializer<List<Float>>(ListSerializer(Float.serializer())) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return if (element is JsonPrimitive) {
            JsonArray(listOf(element))
        } else {
            element
        }
    }
}