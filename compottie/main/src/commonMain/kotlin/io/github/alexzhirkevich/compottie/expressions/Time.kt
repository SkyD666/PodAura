@file:Suppress("INVISIBLE_REFERENCE")

package io.github.alexzhirkevich.compottie.expressions

import io.github.alexzhirkevich.compottie.internal.animation.expressions.state
import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.js.FunctionParam
import io.github.alexzhirkevich.keight.js.JSFunction
import io.github.alexzhirkevich.keight.js.js

internal fun JsFramesToTime() = JSFunction(
    FunctionParam("frame", default = Expression { it.state.frame.js }),
    FunctionParam("fps", default = Expression { (1f / it.state.composition.frameRate).js }),
) {
    val frame = (it[0]?.toKotlin(this) as Number).toFloat()
    val fps = (it[1]?.toKotlin(this) as Number).toFloat()
    (frame / fps).js
}

internal fun JsTimeToFrames() = JSFunction(
    FunctionParam("time", default = Expression {
        (it.state.time.inWholeMilliseconds / 100f + it.state.thisComp.startTime).js
    }),
    FunctionParam("fps", default = Expression { (1f / it.state.composition.frameRate).js }),
    FunctionParam("isDuration", default = Expression { false.js })
) {
    val time = (it[0]?.toKotlin(this) as Number).toFloat()
    val fps = (it[1]?.toKotlin(this) as Number).toFloat()
    val isDuration = (it[2]?.toKotlin(this) as Boolean)

    if (isDuration) {
        (state.absoluteTime.inWholeMilliseconds / 100f + time) * fps
    } else {
        time * fps
    }.js
}

