@file:Suppress("UNCHECKED_CAST")

package io.github.alexzhirkevich.compottie.internal.animation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.util.fastMap
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.Undefined
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable(with = AnimatedColorSerializer::class)
public sealed class AnimatedColor : ExpressionProperty<Color>() {

    internal abstract fun copy(): AnimatedColor

    override fun mapEvaluated(e: Any): Color = Color(mapColor(e))

    override fun mapColor(e: Any): Long {
        return when (e) {
            is Color -> e.toColorLong()
            is FloatArray -> e.toColor().toColorLong()
            is List<*> -> (e as List<Number>).toColor2().toColorLong()
            else -> error("Can't convert $e to color")
        }
    }

    @Serializable
    public class Default(
        @SerialName("k")
        public val value: FloatArray,

        @SerialName("x")
        override val expression: String? = null,

        @SerialName("ix")
        override val index: Int? = null,

        public val sid: String? = null,
    ) : AnimatedColor() {

        @Transient
        private val color: Color = value.toColor()

        override fun copy(): AnimatedColor {
            return Default(
                value = value,
                expression = expression,
                index = index,
                sid = sid
            )
        }

        override fun raw(state: AnimationState): Color = color

        override fun rawColor(state: AnimationState): Long {
            return if (sid != null) {
                state.composition.slotResolver.color(sid, state)
                    ?.interpolatedColor(state)
                    ?: color.toColorLong()
            } else {
                color.toColorLong()
            }
        }
    }

    @Serializable
    public class Animated(
        @SerialName("k")
        override val keyframes: List<VectorKeyframe>,

        @SerialName("x")
        override val expression: String? = null,

        @SerialName("ix")
        override val index: Int? = null,

        public val sid: String? = null,
    ) : AnimatedColor(), RawKeyframeProperty<Color, VectorKeyframe> {

        @Transient
        private val delegate = ColorKeyframeAnimation(
            index = index,
            keyframes = keyframes.fastMap {
                ColorKeyframe(
                    start = it.start?.toColor(),
                    end = it.end?.toColor(),
                    time = it.time,
                    hold = it.hold,
                    inValue = it.inValue,
                    outValue = it.outValue
                )
            },
            emptyValue = Color.Transparent,
            map = { s, e, p ->
                lerp(s, e, easingX.transform(p)).toColorLong()
            }
        )

        override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
            return super<AnimatedColor>.get(property, runtime).let {
                if (it is Undefined)
                    super<RawKeyframeProperty>.get(property, runtime)
                else it
            }
        }

        override fun copy(): AnimatedColor {
            return Animated(
                keyframes = keyframes,
                expression = expression,
                index = index,
                sid = sid
            )
        }

        override fun raw(state: AnimationState): Color {
            return Color(rawColor(state))
        }

        override fun rawColor(state: AnimationState): Long {
            return if (sid != null) {
                state.composition.slotResolver.color(sid, state)
                    ?.interpolatedColor(state)
                    ?: delegate.rawColor(state)
            } else delegate.rawColor(state)
        }
    }
}

internal fun Color.toColorLong(): Long {
    return (value shr 32).toLong()
}

internal fun FloatArray.toColor() = Color(
    red = get(0).toColorComponent(),
    green = get(1).toColorComponent(),
    blue = get(2).toColorComponent(),
    alpha = getOrNull(3)?.toColorComponent() ?: 1f
)

internal fun List<Number>.toColor2() = Color(
    red = get(0).toFloat().toColorComponent(),
    green = get(1).toFloat().toColorComponent(),
    blue = get(2).toFloat().toColorComponent(),
    alpha = getOrNull(3)?.toFloat()?.toColorComponent() ?: 1f
)

// Modern Lotties (v 4.1.9+) have color components in the [0, 1] range.
// Older ones have components in the [0, 255] range.
private fun Float.toColorComponent(): Float = when (this) {
    in COLOR_RANGE_01 -> this
    in COLOR_RANGE_0255 -> this / 255f
    else -> this // will likely throw error of invalid color space
}

private val COLOR_RANGE_01 = 0f .. 1f
private val COLOR_RANGE_0255 = 0f .. 255f


internal object AnimatedColorSerializer : JsonContentPolymorphicSerializer<AnimatedColor>(
    baseClass = AnimatedColor::class
) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<AnimatedColor> {

        check(element is JsonObject) {
            "Invalid color: $element"
        }

        val k = requireNotNull(element.jsonObject["k"]) {
            "Animated shape must have 'k' parameter"
        }

        val animated = element.jsonObject["a"]?.jsonPrimitive?.intOrNull == 1 ||
                k is JsonArray && k[0] is JsonObject

        return if (animated) {
            AnimatedColor.Animated.serializer()
        } else {
            AnimatedColor.Default.serializer()
        }
    }

}

