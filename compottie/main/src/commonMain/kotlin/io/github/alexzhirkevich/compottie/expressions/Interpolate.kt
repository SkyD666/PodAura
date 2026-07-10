package io.github.alexzhirkevich.compottie.expressions

import androidx.compose.animation.core.Easing
import androidx.compose.ui.util.lerp
import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.js

internal fun JSInterpolate(
    easing: Easing
) = Callable {
    val t = toNumber(it[0]).toFloat()

    val (tMin, tMax) = if (it.size < 5)
        0f to 1f
    else toNumber(it[1]).toFloat() to toNumber(it[2]).toFloat()

    val (v1, v2) = if (it.size < 5) it[1] to it[2] else it[3] to it[4]

    interpolate(
        value1 = v1,
        value2 = v2,
        fraction = easing.transform((t - tMin) / (tMax - tMin))
    )
}

private suspend fun ScriptRuntime.interpolate(value1: JsAny?, value2: JsAny?, fraction: Float): JsAny? {
    return when {
        value1 is List<*> && value2 is List<*> ->
            List(minOf(value1.size, value2.size)) {
                interpolate(value1[it] as JsAny?, value2[it] as JsAny?, fraction)
            }.js

        else -> lerp(toNumber(value1).toFloat(), toNumber(value2).toFloat(), fraction).js
    }
}