package io.github.alexzhirkevich.compottie.internal.animation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.util.lerp
import io.github.alexzhirkevich.compottie.dynamic.PropertyProvider
import io.github.alexzhirkevich.compottie.dynamic.map
import io.github.alexzhirkevich.compottie.dynamic.toOffset
import io.github.alexzhirkevich.compottie.dynamic.toScaleFactor
import io.github.alexzhirkevich.compottie.dynamic.toSize
import io.github.alexzhirkevich.compottie.dynamic.toVec2
import io.github.alexzhirkevich.compottie.internal.AnimationState
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.hypot

internal typealias Vec2 = Offset

internal fun Vec2(x: Float, y: Float): Vec2 = Offset(x, y)

@Serializable(with = AnimatedVector2Serializer::class)
public sealed class AnimatedVector2 : DynamicProperty<Vec2>() {


    override fun mapEvaluated(e: Any): Vec2 = Vec2(mapVec(e))

    override fun mapVec(e: Any): Long {
        return when (e) {
            is Vec2 -> e.packedValue
            is FloatArray -> Vec2((e[0] as Number).toFloat(), (e[1] as Number).toFloat()).packedValue
            is List<*> -> Vec2((e[0] as Number).toFloat(), (e[1] as Number).toFloat()).packedValue
            else -> error("Failed to cast $e to Vec2")
        }
    }

    internal abstract fun copy(): AnimatedVector2


    @Serializable
    public class Default(
        @SerialName("k")
        public val value: FloatArray,

        @SerialName("x")
        override val expression: String? = null,

        @SerialName("ix")
        override val index: Int? = null,

        public val sid: String? = null,
    ) : AnimatedVector2() {


        @Transient
        private val vec = Vec2(value[0], value[1])

        override fun raw(state: AnimationState): Vec2 = Vec2(rawVec(state))

        override fun rawVec(state: AnimationState): Long {
            return if (sid != null) {
                state.composition.slotResolver.vector(sid, state)
                    ?.interpolatedVec(state)
                    ?: vec.packedValue
            } else {
                vec.packedValue
            }
        }

        override fun copy(): AnimatedVector2 {
            return Default(
                value = value,
                expression = expression,
                index = index,
                sid = sid
            )
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
    ) : AnimatedVector2(), AnimatedKeyframeProperty<Vec2, VectorKeyframe> {

        private val path = Path()

        private val pathMeasure = PathMeasure()

        @Transient
        private val delegate = VectorKeyframeAnimation(
            index = index,
            keyframes = keyframes,
            emptyValue = Offset.Zero,
            map = { s, e, p ->
                if (inTangent != null && outTangent != null && !s.contentEquals(e)) {
                    path.rewind()
                    path.createPath(s, e, outTangent, inTangent)
                    pathMeasure.setPath(path, false)

                    val length = pathMeasure.length

                    val distance: Float = easingX.transform(p) * length

                    val pos = pathMeasure.getPosition(distance)
                    val tangent = pathMeasure.getTangent(distance)

                    when {
                        distance < 0 -> pos + tangent * distance
                        distance > length -> pos + tangent * (distance - length)
                        else -> pos
                    }.packedValue
                } else {
                    Offset(
                        lerp(s[0], e[0], easingX.transform(p)),
                        lerp(s[1], e[1], easingY.transform(p))
                    ).packedValue
                }
            }
        )

        override fun raw(state: AnimationState): Offset {
            return Offset(rawVec(state))
        }

        override fun rawVec(state: AnimationState): Long {
            return if (sid != null) {
                state.composition.slotResolver.vector(sid, state)
                    ?.interpolatedVec(state)
                    ?: delegate.rawVec(state)
            } else {
                delegate.rawVec(state)
            }
        }

        override fun copy(): AnimatedVector2 {
            return Animated(
                keyframes = keyframes,
                expression = expression,
                index = index,
                sid = sid
            )
        }
    }

    @Serializable
    public class Split(
        public val x: AnimatedNumber,
        public val y: AnimatedNumber,
    ) : AnimatedVector2() {


        override val expression: String?
            get() = null

        override val index: Int? = null

        override fun copy(): AnimatedVector2 {
            return Split(x.copy(), y.copy())
        }

        override fun raw(state: AnimationState): Vec2 = Vec2(rawVec(state))

        override fun rawVec(state: AnimationState): Long {
            return Vec2(
                x.interpolatedFloat(state),
                y.interpolatedFloat(state)
            ).packedValue
        }
    }
}

internal fun AnimatedVector2.Companion.defaultPosition(): AnimatedVector2 =
    AnimatedVector2.Default(FloatList3)

internal fun AnimatedVector2.Companion.defaultAnchorPoint(): AnimatedVector2 =
    AnimatedVector2.Default(FloatList3)

internal fun AnimatedVector2.Companion.defaultScale(): AnimatedVector2 =
    AnimatedVector2.Default(FloatList3_100)

private val FloatList3 = floatArrayOf(0f, 0f, 0f)
private val FloatList3_100 = floatArrayOf(100f, 100f, 100f)


internal fun AnimatedVector2.interpolatedNorm(state: AnimationState): Long =
    (Vec2(interpolatedVec(state)) / 100f).packedValue

internal fun AnimatedVector2.dynamicOffset(
    provider: PropertyProvider<Offset>?
) {
    dynamic = provider?.map(from = Offset::toVec2, to = Vec2::toOffset)
}

internal fun AnimatedVector2.dynamicSize(
    provider: PropertyProvider<Size>?
) {
    dynamic = provider?.map(from = Size::toVec2, to = Vec2::toSize)
}

internal fun AnimatedVector2.dynamicScale(
    provider: PropertyProvider<ScaleFactor>?
) {
    dynamic = provider?.map(from = ScaleFactor::toVec2, to = Vec2::toScaleFactor)
}

internal object AnimatedVector2Serializer : JsonContentPolymorphicSerializer<AnimatedVector2>(AnimatedVector2::class) {

    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<AnimatedVector2> {

        check(element is JsonObject) {
            "Invalid vector: $element"
        }
        val k = element["k"]

        return when {

            element["s"]?.jsonPrimitive?.booleanOrNull == true ->
                AnimatedVector2.Split.serializer()

            element["a"]?.jsonPrimitive?.intOrNull == 1 ||
                    k is JsonArray && k[0] is JsonObject ->
                AnimatedVector2.Animated.serializer()

            else -> AnimatedVector2.Default.serializer()
        }
    }
}


private fun Path.createPath(
    startPoint: FloatArray,
    endPoint: FloatArray,
    cp1: FloatArray,
    cp2: FloatArray
) {
    moveTo(startPoint[0], startPoint[1])

    if ((cp1.hypot() != 0f || cp2.hypot() != 0f)) {
        cubicTo(
            startPoint[0] + cp1[0],
            startPoint[1] + cp1[1],
            endPoint[0] + cp2[0],
            endPoint[1] + cp2[1],
            endPoint[0], endPoint[1]
        )
    } else {
        lineTo(endPoint[0], endPoint[1])
    }
}

private fun FloatArray.hypot() = hypot(this[0], this[1])