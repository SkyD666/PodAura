package io.github.alexzhirkevich.compottie.internal.animation.expressions

import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.keight.ScriptEngine
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.findRoot
import kotlin.coroutines.CoroutineContext

public interface ExpressionsRuntime : ScriptRuntime {
    public val state: AnimationState
}

public typealias ExpressionsEngineFactory = (CoroutineContext, AnimationState) -> ScriptEngine<ExpressionsRuntime>

internal val ScriptRuntime.state: AnimationState get() = (findRoot() as ExpressionsRuntime).state

internal fun Any.toJs(runtime: ScriptRuntime) = runtime.fromKotlin(this)