package io.github.alexzhirkevich.compottie.internal.animation

import androidx.compose.ui.util.lerp
import io.github.alexzhirkevich.compottie.dynamic.PropertyProvider
import io.github.alexzhirkevich.compottie.dynamic.invoke
import io.github.alexzhirkevich.compottie.internal.AnimationState
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
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

@Serializable(with = AnimatedNumberSerializer::class)
public sealed class AnimatedNumber : DynamicProperty<Float>() {

    internal abstract fun copy(): AnimatedNumber

    override fun mapEvaluated(e: Any): Float = mapFloat(e)

    override fun mapFloat(e: Any): Float {
        return when (e) {
            is Number -> e.toFloat()
            is List<*> -> (e[0] as Number).toFloat()
            else -> error("Failed to cast $e to number")
        }
    }

    @Serializable
    public class Default(
        @SerialName("k")
        @Serializable(with = ValueSerializer::class)
        public val value: Float,

        @SerialName("x")
        override val expression: String? = null,

        @SerialName("ix")
        override val index: Int? = null,

        public val sid: String? = null,
    ) : AnimatedNumber() {

        override fun copy(): AnimatedNumber {
            return Default(
                value = value,
                expression = expression,
                index = index,
                sid = sid
            )
        }

        override fun raw(state: AnimationState): Float = rawFloat(state)

        override fun rawFloat(state: AnimationState): Float {
            return if (sid != null) {
                state.composition.slotResolver.number(sid, state)
                    ?.interpolatedFloat(state)
                    ?: value
            } else {
                value
            }
        }
    }

    @Serializable
    public class Animated(
        @SerialName("k")
        override val keyframes: List<ValueKeyframe>,

        @SerialName("x")
        override val expression: String? = null,

        @SerialName("ix")
        override val index: Int? = null,

        public val sid: String? = null,
    ) : AnimatedNumber(), AnimatedKeyframeProperty<Float, ValueKeyframe> {

        @Transient
        private val delegate = ValueKeyframeAnimation(
            index = index,
            keyframes = keyframes,
            emptyValue = 1f,
            map = { s, e, p ->
                lerp(s[0], e[0], easingX.transform(p))
            }
        )

        override fun copy(): AnimatedNumber {
            return Animated(
                keyframes = keyframes,
                expression = expression,
                index = index,
                sid = sid
            )
        }

        override fun raw(state: AnimationState): Float {
            return rawFloat(state)
        }

        override fun rawFloat(state: AnimationState): Float {
            return if (sid != null) {
                state.composition.slotResolver.number(sid, state)
                    ?.interpolatedFloat(state)
                    ?: delegate.rawFloat(state)
            } else {
                delegate.rawFloat(state)
            }
        }
    }
}

internal fun AnimatedNumber.dynamicNorm(provider: PropertyProvider<Float>?) {
    dynamic = if (provider != null) PropertyProvider {
        provider.invoke(this, it) * 100f
    } else null
}

internal fun AnimatedNumber.Companion.defaultRotation(): AnimatedNumber =
    AnimatedNumber.Default(0f)

internal fun AnimatedNumber.Companion.defaultSkew(): AnimatedNumber =
    AnimatedNumber.Default(0f)

internal fun AnimatedNumber.Companion.defaultSkewAxis(): AnimatedNumber =
    AnimatedNumber.Default(0f)

internal fun AnimatedNumber.Companion.defaultOpacity(): AnimatedNumber =
    AnimatedNumber.Default(100f)

internal fun AnimatedNumber.Companion.defaultRoundness(): AnimatedNumber =
    AnimatedNumber.Default(0f)

internal fun AnimatedNumber.Companion.defaultRadius(): AnimatedNumber =
    AnimatedNumber.Default(0f)


internal fun AnimatedNumber.interpolatedNorm(state: AnimationState) = interpolatedFloat(state) / 100f

internal object ValueSerializer : JsonTransformingSerializer<Float>(Float.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return when (element) {
            is JsonArray -> element[0]
            else -> element
        }
    }
}

internal object AnimatedNumberSerializer : JsonContentPolymorphicSerializer<AnimatedNumber>(AnimatedNumber::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<AnimatedNumber> {

        if (element is JsonPrimitive) {
            return AnimatedNumberAsPrimitiveSerializer
        }

        val value = requireNotNull(element.jsonObject["k"]) {
            "Illegal animated number encoding: $element"
        }

        val animated = element.jsonObject["a"]?.jsonPrimitive?.intOrNull == 1 ||
                value is JsonObject || value is JsonArray && value.firstOrNull() is JsonObject

        return if (animated) {
            AnimatedNumber.Animated.serializer()
        } else {
            AnimatedNumber.Default.serializer()
        }
    }
}

private object AnimatedNumberAsPrimitiveSerializer :
    JsonTransformingSerializer<AnimatedNumber.Default>(AnimatedNumber.Default.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return JsonObject(mapOf("k" to element))
    }
}