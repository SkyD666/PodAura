@file:Suppress("UNCHECKED_CAST")

package io.github.alexzhirkevich.compottie.internal.animation

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.animation.expressions.onTime
import io.github.alexzhirkevich.compottie.internal.animation.expressions.state
import io.github.alexzhirkevich.compottie.internal.animation.expressions.toJs
import io.github.alexzhirkevich.compottie.internal.helpers.Bezier
import io.github.alexzhirkevich.compottie.internal.isNotNull
import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsAny
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable(with = AnimatedShapeSerializer::class)
public sealed class AnimatedShape : AnimatedProperty<Path>, ExpressionHolder {

    @Transient
    override val jsCache: MutableMap<String, JsAny?> = HashMap()

    override var group: PropertyGroup? = null

    internal abstract fun rawBezier(state: AnimationState): Bezier

    internal abstract fun copy(): AnimatedShape

    internal abstract fun setClosed(closed: Boolean)

    override fun prepareExpressions(state: AnimationState) {

    }

    private suspend fun createPath(args: List<JsAny?>, runtime: ScriptRuntime): AnimatedShape {
        val points = (args[0]?.toKotlin(runtime) as? List<List<Number>>)
            ?.fastMap { it.fastMap { it.toFloat() }.toFloatArray() }
            ?: DefaultPoints

        val inTangents = (args.getOrNull(1)?.toKotlin(runtime) as? List<List<Number>>)
            ?.fastMap { l -> FloatArray(l.size) { l[it].toFloat() } }
            ?: emptyList()

        val outTangents = (args.getOrNull(2)?.toKotlin(runtime) as? List<List<Number>>)
            ?.fastMap { l -> FloatArray(l.size) { l[it].toFloat() } }
            ?: emptyList()

        val isClosed = !runtime.isFalse(args.getOrNull(3) ?: true)

        return Default(
            bezier = Bezier(
                vertices = points,
                inTangents = inTangents,
                outTangents = outTangents,
                isClosed = isClosed
            )
        )
    }
//
//    private fun pointOnPath(perc: Float, time: Float) : Vec2 {
//
//        var shapePath = this.v;
//        if (time !== undefined) {
//            shapePath = this.getValueAtTime(time, 0);
//        }
//        if (!this._segmentsLength) {
//            this._segmentsLength = bez.getSegmentsLength(shapePath);
//        }
//
//        var segmentsLength = this._segmentsLength;
//        var lengths = segmentsLength.lengths;
//        var lengthPos = segmentsLength.totalLength * perc;
//        var i = 0;
//        var len = lengths.length;
//        var accumulatedLength = 0;
//        var pt;
//        while (i < len) {
//            if (accumulatedLength + lengths[i].addedLength > lengthPos) {
//                var initIndex = i;
//                var endIndex = (shapePath.c && i === len - 1) ? 0 : i+1;
//                var segmentPerc = (lengthPos - accumulatedLength) / lengths[i].addedLength;
//                pt = bez.getPointInSegment(
//                    shapePath.v[initIndex],
//                    shapePath.v[endIndex],
//                    shapePath.o[initIndex],
//                    shapePath.i[endIndex],
//                    segmentPerc,
//                    lengths[i]
//                );
//                break;
//            } else {
//                accumulatedLength += lengths[i].addedLength;
//            }
//            i += 1;
//        }
//        if (!pt) {
//            pt =
//                shapePath.c ? [shapePath.v[0][0], shapePath.v[0][1]] : [shapePath.v[shapePath._length-1][0], shapePath.v[shapePath._length-1][1]];
//        }
//        return pt;
//    }
//    private fun vectorOnPath(perc : Float, time : Float, state: AnimationState) : List<Float>{
//        // perc doesn't use triple equality because it can be a Number object as well as a primitive.
//        val p  : Float = if (perc == 1f) { // eslint-disable-line eqeqeq
//            this.v.c;
//        } else if (perc == 0f) { // eslint-disable-line eqeqeq
//            0.999f;
//        } else perc
//
//        val pt1 = this.pointOnPath(perc, time);
//        val pt2 = this.pointOnPath(perc + 0.001f, time);
//        val xLength = pt2.x - pt1.x;
//        val yLength = pt2.y - pt1.y;
//        val magnitude = Math.sqrt(Math.pow(xLength, 2f) + Math.pow(yLength, 2.0));
//        if (magnitude == 0f) {
//            return listOf(0f, 0f);
//        }
//        var unitVector = vectorType === 'tangent' ? [xLength / magnitude, yLength / magnitude] : [-yLength / magnitude, xLength / magnitude];
//        return unitVector;
//    }

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
        return when (property?.toString()) {
            "points" -> Callable {
                onTime(it.getOrNull(0)) {
                    rawBezier(state).vertices.toJs(runtime)
                }
            }

            "inTangents" -> Callable {
                onTime(it.getOrNull(0)) {
                    rawBezier(state).inTangents.toJs(runtime)
                }
            }

            "outTangents" -> Callable {
                onTime(it.getOrNull(0)) {
                    rawBezier(state).outTangents.toJs(runtime)
                }
            }

            "isClosed" -> Callable { rawBezier(state).isClosed.toJs(this) }
            "createPath" -> Callable { createPath(it, this) }
            else -> super.get(property, runtime)
        }
    }


    @Serializable
    public class Default(
        @SerialName("x")
        public val expression: String? = null,

        @SerialName("ix")
        override val index: Int? = null,

        @SerialName("k")
        public val bezier: Bezier,
    ) : AnimatedShape() {

        @Transient
        private val path = Path().apply {
            bezier.mapPath(this)
        }

        override fun setClosed(closed: Boolean) {
            bezier.setIsClosed(closed)
            bezier.mapPath(path)
        }

        override fun rawBezier(state: AnimationState): Bezier {
            return bezier
        }

        override fun raw(state: AnimationState): Path {
            return path
        }

        override fun copy(): AnimatedShape {
            return Default(
                expression = expression,
                index = index,
                bezier = bezier
            )
        }
    }

    @Serializable
    public class Animated(
        @SerialName("x")
        public val expression: String? = null,

        @SerialName("ix")
        override val index: Int? = null,

        @SerialName("k")
        override val keyframes: List<BezierKeyframe>,
    ) : AnimatedShape(), AnimatedKeyframeProperty<Path, BezierKeyframe> {

        @Transient
        private val tmpPath = Path()

        @Transient
        private val tmpBezier = Bezier()

        @Transient
        private var bezierDelegate = BaseKeyframeAnimation(
            index = index,
            sourceKeyframes = keyframes,
            emptyValue = tmpBezier,
            map = { s, e, p ->
                tmpBezier.interpolateBetween(s, e, easingX.transform(p))
                tmpBezier
            },
        )

        override fun setClosed(closed: Boolean) {
            keyframes.fastForEach {
                it.start?.setIsClosed(closed)
                it.end?.setIsClosed(closed)
            }
        }

        override fun rawBezier(state: AnimationState): Bezier {
            return bezierDelegate.tween(
                state = state,
                default = bezierDelegate::raw,
                fromKeyframe = { it },
                lerp = { s, e, p ->
                    tmpBezier.interpolateBetween(s, e, p)
                    tmpBezier
                }
            )
        }

        override fun copy(): AnimatedShape {
            return Animated(
                expression = expression,
                index = index,
                keyframes = keyframes
            )
        }

        override fun raw(state: AnimationState): Path {
            rawBezier(state).mapPath(tmpPath)
            return tmpPath
        }

        override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
            return super<AnimatedKeyframeProperty>.get(property, runtime)
        }
    }

    @Serializable
    public class Slottable(
        private val sid: String,
        @SerialName("ix")
        override val index: Int? = null,
    ) : AnimatedShape() {

        private val EmptyPath by lazy { Path() }
        private val EmptyBezier by lazy { Bezier() }

        override fun rawBezier(state: AnimationState): Bezier {
            return state.composition.slotResolver.shape(sid, state)
                ?.rawBezier(state)
                ?: EmptyBezier
        }

        override fun copy(): AnimatedShape {
            return Slottable(sid, index)
        }

        override fun setClosed(closed: Boolean) {}

        override fun raw(state: AnimationState): Path {
            return state.composition.slotResolver.shape(sid, state)
                ?.interpolated(state)
                ?: EmptyPath.apply { reset() }
        }
    }
}

internal object AnimatedShapeSerializer : JsonContentPolymorphicSerializer<AnimatedShape>(
    baseClass = AnimatedShape::class
) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<AnimatedShape> {

        if (element.jsonObject["sid"].isNotNull())
            return AnimatedShape.Slottable.serializer()

        val k = requireNotNull(element.jsonObject["k"]) {
            "Animated shape must have 'k' parameter"
        }

        return if (element.jsonObject["a"]?.jsonPrimitive?.intOrNull == 0 || k is JsonObject) {
            AnimatedShape.Default.serializer()
        } else {
            AnimatedShape.Animated.serializer()
        }
    }
}


private val DefaultPoints by lazy {
    listOf(
        floatArrayOf(0f, 0f),
        floatArrayOf(100f, 0f),
        floatArrayOf(100f, 100f),
        floatArrayOf(0f, 100f)
    )
}


