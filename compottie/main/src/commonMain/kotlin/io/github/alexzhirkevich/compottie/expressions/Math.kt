@file:Suppress("INVISIBLE_REFERENCE", "UNCHECKED_CAST")

package io.github.alexzhirkevich.compottie.expressions

import io.github.alexzhirkevich.compottie.internal.utils.degreeToRadians
import io.github.alexzhirkevich.compottie.internal.utils.radiansToDegree
import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.js.FunctionParam
import io.github.alexzhirkevich.keight.js.JSFunction
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.js
import kotlin.math.hypot

internal fun JsClamp() = JSFunction(
    FunctionParam("v"),
    FunctionParam("from"),
    FunctionParam("to"),
) {
    val v = it[0]?.toKotlin(this) as Number
    val from = it[1]?.toKotlin(this) as Number
    val to = it[2]?.toKotlin(this) as Number

    if (v is Long && from is Long && to is Long) {
        v.coerceIn(from, to).js
    } else {
        v.toDouble().coerceIn(from.toDouble(), to.toDouble()).js
    }
}

internal fun JsDegreesToRadians() = JSFunction(FunctionParam("deg")) {
    degreeToRadians((it[0]?.toKotlin(this) as Number).toDouble()).js
}

internal fun JSRadiansToDegrees() = JSFunction(FunctionParam("rad")) {
    radiansToDegree((it[0]?.toKotlin(this) as Number).toDouble()).js
}

internal fun JsDot() = Callable {
    val a = it[0] as List<JsAny>
    val b = it[1] as List<JsAny>
    val res = toNumber(a[0]).toFloat() * toNumber(b[0]).toFloat() +
            toNumber(a[1]).toFloat() * toNumber(b[1]).toFloat()
    res.js
}

internal fun JsLength() = Callable {
    val a = it[0]
    val b = it.getOrNull(1)

    return@Callable when (a) {
        is List<*> if b == null -> {
            a as List<JsAny>
            hypot((toNumber(a[0])).toFloat(), toNumber(a[1]).toFloat()).js
        }

        is List<*> if b is List<*> -> {
            a as List<JsAny>
            b as List<JsAny>
            hypot(
                toNumber(b[0]).toFloat() - toNumber(a[0]).toFloat(),
                toNumber(b[1]).toFloat() - toNumber(a[1]).toFloat()
            ).js
        }

        else -> error("Invalid arguments for length()")
    }
}

internal fun JsNormalize(): Callable {
    val length = JsLength()
    return Callable { div(it[0], length.invoke(it, this)) }
}