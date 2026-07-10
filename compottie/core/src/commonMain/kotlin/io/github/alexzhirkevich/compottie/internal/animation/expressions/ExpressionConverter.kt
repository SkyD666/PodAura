package io.github.alexzhirkevich.compottie.internal.animation.expressions

import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.findRoot
import io.github.alexzhirkevich.keight.js.JsAny

//internal val Vec2.js get() = listOf(x.js, y.js).js
//internal val Color.js get() = listOf(red.js, green.js, blue.js, alpha.js).js

//internal val List<List<Number>>.js
//    get() = fastMap { it.fastMap { it.js }.js }.js


internal fun <T> ScriptRuntime.onTime(time: JsAny?, block: (AnimationState) -> T): T {
    val t = (time?.toKotlin(this) as? Number)?.toFloat()
    val state = (findRoot() as ExpressionsRuntime).state
    return if (t == null) {
        block(state)
    } else {
        state.onTime(t, block)
    }
}

