package io.github.alexzhirkevich.compottie.internal.animation

import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.animation.expressions.JSLoopIn
import io.github.alexzhirkevich.compottie.internal.animation.expressions.JSTemporalWiggle
import io.github.alexzhirkevich.compottie.internal.animation.expressions.JSWiggle
import io.github.alexzhirkevich.compottie.internal.animation.expressions.JsLoopOut
import io.github.alexzhirkevich.compottie.internal.animation.expressions.JsSmooth
import io.github.alexzhirkevich.compottie.internal.animation.expressions.onTime
import io.github.alexzhirkevich.compottie.internal.animation.expressions.state
import io.github.alexzhirkevich.compottie.internal.animation.expressions.toJs
import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.Undefined

public interface RawProperty<T : Any> : JsAny {

    public val index: Int?

    public val jsCache: MutableMap<String, JsAny?>

    public var group: PropertyGroup?

    public fun raw(state: AnimationState): T

    public fun rawFloat(state: AnimationState): Float = raw(state) as Float
    public fun rawVec(state: AnimationState): Long = raw(state) as Long
    public fun rawColor(state: AnimationState): Long = raw(state) as Long

    override suspend fun keys(
        runtime: ScriptRuntime,
        excludeSymbols: Boolean,
        excludeNonEnumerables: Boolean
    ): List<JsAny?> = listOf(
        "value".toJs(runtime),
        "valueAtTime".toJs(runtime),
        "propertyIndex".toJs(runtime),
        "propertyGroup".toJs(runtime),
        "transform".toJs(runtime),
        "loopIn".toJs(runtime),
        "loopInDuration".toJs(runtime),
        "loopOut".toJs(runtime),
        "loopOutDuration".toJs(runtime),
        "wiggle".toJs(runtime),
        "temporalWiggle".toJs(runtime),
        "smooth".toJs(runtime),
    )

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
        return when (property?.toString()) {
            "value" -> raw(runtime.state).toJs(runtime)
            "valueAtTime" -> jsCache.getOrPut("valueAtTime") {
                Callable { onTime(it.getOrNull(0)) { raw(it).toJs(runtime) } }
            }

            "propertyIndex" -> index?.toJs(runtime) ?: Undefined
            "propertyGroup" -> jsCache.getOrPut("propertyGroup") {
                Callable {
                    var n = toNumber(it.getOrNull(0) ?: return@Callable Undefined).toInt()
                    var g: PropertyGroup? = group
                    while (--n > 0) {
                        g = g?.group
                    }
                    g
                }
            }

            "transform" -> group
            "loopIn" -> jsCache.getOrPut("loopIn") { JSLoopIn(false) }
            "loopInDuration" -> jsCache.getOrPut("loopInDuration") { JSLoopIn(true) }
            "loopOut" -> jsCache.getOrPut("loopOut") { JsLoopOut(false) }
            "loopOutDuration" -> jsCache.getOrPut("loopOutDuration") { JsLoopOut(true) }
            "wiggle" -> jsCache.getOrPut("wiggle") { JSWiggle(this) }
            "temporalWiggle" -> jsCache.getOrPut("temporalWiggle") { JSTemporalWiggle(this) }
            "smooth" -> jsCache.getOrPut("smooth") { JsSmooth(this) }
            else -> super.get(property, runtime)
        }
    }
}

internal interface RawKeyframeProperty<T : Any, K : Keyframe<*>> : RawProperty<T> {

    val keyframes: List<K>

    override suspend fun keys(
        runtime: ScriptRuntime,
        excludeSymbols: Boolean,
        excludeNonEnumerables: Boolean
    ): List<JsAny?> = super.keys(runtime, excludeSymbols, excludeNonEnumerables) +
            "numKeys".toJs(runtime)

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
        return when (property?.toString()) {
            "numKeys" -> keyframes.size.toJs(runtime)
            else -> super.get(property, runtime)
        }
    }
}

internal interface AnimatedProperty<T : Any> : RawProperty<T> {

    /**
     * Property value interpolation including dynamics and expressions.
     * Should be called from the DrawScope. Calling from expressions can overflow the stack.
     * */
    fun interpolated(state: AnimationState): T = raw(state)

    fun interpolatedFloat(state: AnimationState): Float = interpolated(state) as Float
    fun interpolatedVec(state: AnimationState): Long = interpolated(state) as Long
    fun interpolatedColor(state: AnimationState): Long = interpolated(state) as Long
}

internal interface AnimatedKeyframeProperty<T : Any, K : Keyframe<*>>
    : AnimatedProperty<T>, RawKeyframeProperty<T, K>