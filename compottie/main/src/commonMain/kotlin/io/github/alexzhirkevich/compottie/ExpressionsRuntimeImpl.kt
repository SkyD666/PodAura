@file:Suppress("INVISIBLE_REFERENCE", "UNCHECKED_CAST")

package io.github.alexzhirkevich.compottie

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.graphics.Color
import io.github.alexzhirkevich.compottie.expressions.JSHexToRgb
import io.github.alexzhirkevich.compottie.expressions.JSHslToRgb
import io.github.alexzhirkevich.compottie.expressions.JSInterpolate
import io.github.alexzhirkevich.compottie.expressions.JSNoise
import io.github.alexzhirkevich.compottie.expressions.JSRadiansToDegrees
import io.github.alexzhirkevich.compottie.expressions.JSRandomNumber
import io.github.alexzhirkevich.compottie.expressions.JSRgbToHsl
import io.github.alexzhirkevich.compottie.expressions.JSSeedRandom
import io.github.alexzhirkevich.compottie.expressions.JsClamp
import io.github.alexzhirkevich.compottie.expressions.JsDegreesToRadians
import io.github.alexzhirkevich.compottie.expressions.JsDot
import io.github.alexzhirkevich.compottie.expressions.JsFramesToTime
import io.github.alexzhirkevich.compottie.expressions.JsLength
import io.github.alexzhirkevich.compottie.expressions.JsNormalize
import io.github.alexzhirkevich.compottie.expressions.JsTimeToFrames
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.animation.Vec2
import io.github.alexzhirkevich.compottie.internal.animation.expressions.ExpressionsRuntime
import io.github.alexzhirkevich.compottie.internal.effects.EffectValue
import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.JSEngine
import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.Script
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsProperty
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.js
import io.github.alexzhirkevich.keight.runSync
import kotlin.coroutines.CoroutineContext
import kotlin.math.max

private val easeIn = CubicBezierEasing(0.333f, 0f, 0.833f, 0.833f)
private val easeOut = CubicBezierEasing(0.167f, 0.167f, 0.667f, 1f)
private val easeInOut = CubicBezierEasing(0.33f, 0f, 0.667f, 1f)

internal class ExpressionsRuntimeImpl(
    context: CoroutineContext,
    override val state: AnimationState
) : JSRuntime(
    context = context,
    isSuspendAllowed = false
), ExpressionsRuntime {

    init {
        runSync {
            set("time".js, JsProperty { (state.time.inWholeMilliseconds / 1_000f).js }, VariableType.Const)
            set("value".js, JsProperty { state.thisProperty?.raw(state)?.let { fromKotlin(it) } }, VariableType.Const)

            set("thisComp".js, JsProperty { state.thisComp }, VariableType.Const)
            set("thisLayer".js, JsProperty { state.thisLayer }, VariableType.Const)
            set("thisProp".js, JsProperty { state.thisProperty }, VariableType.Const)

            set("add".js, Callable { sum(it[0], it[1]) }, VariableType.Const)
            set("sum".js, Callable { sum(it[0], it[1]) }, VariableType.Const)
            set($$"$bm_sum".js, Callable { sum(it[0], it[1]) }, VariableType.Const)
            set("sub".js, Callable { sub(it[0], it[1]) }, VariableType.Const)
            set($$"$bm_sub".js, Callable { sub(it[0], it[1]) }, VariableType.Const)
            set("mul".js, Callable { mul(it[0], it[1]) }, VariableType.Const)
            set($$"$bm_mul".js, Callable { mul(it[0], it[1]) }, VariableType.Const)
            set("div".js, Callable { div(it[0], it[1]) }, VariableType.Const)
            set($$"$bm_div".js, Callable { div(it[0], it[1]) }, VariableType.Const)
            set("mod".js, Callable { mod(it[0], it[1]) }, VariableType.Const)
            set("clamp".js, JsClamp(), VariableType.Const)
            set("dot".js, JsDot(), VariableType.Const)
            set("length".js, JsLength(), VariableType.Const)
            set("normalize".js, JsNormalize(), VariableType.Const)

            set("degreesToRadians".js, JsDegreesToRadians(), VariableType.Const)
            set("radiansToDegrees".js, JSRadiansToDegrees(), VariableType.Const)
            set("rgbToHsl".js, JSRgbToHsl(), VariableType.Const)
            set("hslToRgb".js, JSHslToRgb(), VariableType.Const)
            set("hexToRgb".js, JSHexToRgb(), VariableType.Const)
            set("framesToTime".js, JsFramesToTime(), VariableType.Const)
            set("timeToFrames".js, JsTimeToFrames(), VariableType.Const)

            set("random".js, JSRandomNumber(false), VariableType.Const)
            set("gaussRandom".js, JSRandomNumber(true), VariableType.Const)
            set("setRandom".js, JSSeedRandom(), VariableType.Const)
            set("noise".js, JSNoise(), VariableType.Const)

            set("linear".js, JSInterpolate(LinearEasing), VariableType.Const)
            set("ease".js, JSInterpolate(easeInOut), VariableType.Const)
            set("easeIn".js, JSInterpolate(easeIn), VariableType.Const)
            set("easeOut".js, JSInterpolate(easeOut), VariableType.Const)
        }
    }


    override fun fromKotlin(value: Any): JsAny {
        return when (value) {
            is Vec2 -> super.fromKotlin(listOf(value.x, value.y))
            is Color -> super.fromKotlin(listOf(value.red, value.green, value.blue, value.alpha))
            else -> super.fromKotlin(value)
        }
    }

    override suspend fun contains(property: JsAny?): Boolean {
        return super.contains(property)
                || state.thisProperty?.contains(property, this) == true
                || state.thisLayer.contains(property, this)
                || state.thisComp.contains(property, this)
    }

    override suspend fun get(property: JsAny?): JsAny? {
        state.thisProperty?.let {
            it.get(property, this).also { if (it != Undefined) return it }
        }
        state.thisLayer.get(property, this).also { if (it != Undefined) return it }
        state.thisComp.get(property, this).also { if (it != Undefined) return it }
        super.get(property).also { if (it != Undefined) return it }

        return Undefined
    }

    override suspend fun sum(a: JsAny?, b: JsAny?): JsAny? {
        return when {
            a is EffectValue<*> -> sum(a.value?.raw(state)?.let(::fromKotlin), b)
            b is EffectValue<*> -> sum(a, b.value?.raw(state)?.let(::fromKotlin))
            a is List<*> && b is List<*> -> {
                a as List<JsAny?>
                b as List<JsAny?>
                List(max(a.size, b.size)) {
                    sum(
                        a.getOrElse(it) { 0.js },
                        b.getOrElse(it) { 0.js },
                    )
                }.js
            }

            else -> super.sum(a, b)
        }
    }

    override suspend fun sub(a: JsAny?, b: JsAny?): JsAny? {
        return when {
            a is EffectValue<*> -> sub(a.value?.raw(state)?.let(::fromKotlin), b)
            b is EffectValue<*> -> sub(a, b.value?.raw(state)?.let(::fromKotlin))
            a is List<*> && b is List<*> -> {
                a as List<JsAny?>
                b as List<JsAny?>
                List(max(a.size, b.size)) {
                    sub(
                        a.getOrElse(it) { 0.js },
                        b.getOrElse(it) { 0.js },
                    )
                }.js
            }

            else -> super.sub(a, b)
        }
    }


    override suspend fun div(a: JsAny?, b: JsAny?): JsAny? {
        return when {
            a is EffectValue<*> -> div(a.value?.raw(state)?.let(::fromKotlin), b)
            b is EffectValue<*> -> div(a, b.value?.raw(state)?.let(::fromKotlin))
            a is List<*> -> (a as List<JsAny?>).map { div(it, b) }.js

            else -> super.div(a, b)
        }
    }

    override suspend fun mul(a: JsAny?, b: JsAny?): JsAny? {
        return when {
            a is EffectValue<*> -> mul(a.value?.raw(state)?.let(::fromKotlin), b)
            b is EffectValue<*> -> mul(a, b.value?.raw(state)?.let(::fromKotlin))
            a is List<*> -> (a as List<JsAny?>).map { mul(it, b) }.js
            else -> super.mul(a, b)
        }
    }
}

internal class ExpressionsEngineImpl(
    runtime: ExpressionsRuntimeImpl
) : JSEngine<ExpressionsRuntimeImpl>(runtime) {

    override fun compile(script: String, name: String?): Script {
        val script = super.compile(script, name)
        return object : Script by script {
            override suspend fun invoke(runtime: ScriptRuntime): JsAny? {
                return runtime.withScope(isIsolated = true) {
                    script.invoke(it)
                }
            }
        }
    }
}

