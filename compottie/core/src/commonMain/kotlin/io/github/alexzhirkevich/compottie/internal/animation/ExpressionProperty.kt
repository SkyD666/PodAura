@file:Suppress("UNCHECKED_CAST")

package io.github.alexzhirkevich.compottie.internal.animation

import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.animation.expressions.ExpressionEvaluator
import io.github.alexzhirkevich.compottie.internal.effects.EffectValue
import io.github.alexzhirkevich.keight.js.JsAny
import kotlinx.serialization.Transient

public sealed class ExpressionProperty<T : Any> : AnimatedProperty<T>, ExpressionHolder {

    internal abstract val expression: String?

    override val jsCache: MutableMap<String, JsAny?> = HashMap()

    override var group: PropertyGroup? = null

    @Transient
    private val expressionEvaluator: ExpressionEvaluator? by lazy {
        expression?.let { ExpressionEvaluator(it, this) }
    }

    override fun prepareExpressions(state: AnimationState) {
        expressionEvaluator?.prepareExpressions(state)
    }

    internal abstract fun mapEvaluated(e: Any): T
    internal open fun mapFloat(e: Any): Float = mapEvaluated(e) as Float
    internal open fun mapVec(e: Any): Long = mapEvaluated(e) as Long
    internal open fun mapColor(e: Any): Long = mapEvaluated(e) as Long

    override fun interpolated(state: AnimationState): T {
        return interpolatedInternal(
            state = state,
            raw = { p, s -> p.raw(s) as T },
            interpolated = { p, s -> p.interpolated(s) as T },
            map = ::mapEvaluated
        )
    }

    override fun interpolatedFloat(state: AnimationState): Float {
        return interpolatedInternal(
            state = state,
            raw = { p, s -> p.rawFloat(s) },
            interpolated = { p, s -> p.interpolatedFloat(s) },
            map = ::mapFloat
        )
    }


    override fun interpolatedVec(state: AnimationState): Long {
        return interpolatedInternal(
            state = state,
            raw = { p, s -> p.rawVec(s) },
            interpolated = { p, s -> p.interpolatedVec(s) },
            map = ::mapVec
        )
    }

    override fun interpolatedColor(state: AnimationState): Long {
        return interpolatedInternal(
            state = state,
            raw = { p, s -> p.rawColor(s) },
            interpolated = { p, s -> p.interpolatedColor(s) },
            map = ::mapColor
        )
    }

    private inline fun <R : Any> interpolatedInternal(
        state: AnimationState,
        raw: (RawProperty<*>, AnimationState) -> R,
        interpolated: (AnimatedProperty<*>, AnimationState) -> R,
        map: (Any) -> R
    ): R {
        if (!state.enableExpressions) {
            return raw(this, state)
        }

        val evaluated = expressionEvaluator?.evaluate(state)

        if (evaluated == null) {
            return raw(this, state)
        }

        return when (evaluated) {
            is AnimatedProperty<*> -> if (this === evaluated) {
                raw(evaluated, state)
            } else {
                interpolated(evaluated, state)
            }

            is RawProperty<*> -> raw(evaluated, state)
            is EffectValue<*> -> {
                val v = evaluated.value
                if (v != null) {
                    raw(v, state)
                } else {
                    raw(this, state)
                }
            }

            else -> try {
                map(evaluated)
            } catch (_: Throwable) {
                raw(this, state)
            }
        }
    }
}