@file:Suppress("UNCHECKED_CAST")

package io.github.alexzhirkevich.compottie.internal.animation.expressions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastMap
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.layers.Layer
import io.github.alexzhirkevich.compottie.internal.layers.totalTransformMatrix
import io.github.alexzhirkevich.compottie.internal.timeSeconds
import io.github.alexzhirkevich.compottie.internal.utils.fastSetFrom
import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.Undefined

internal fun JSGetLayerEffect(layer: Layer) = Callable {
    val index = it[0]?.toKotlin(this) ?: return@Callable Undefined

    if (index is Number) {
        val i = index.toInt()
        layer.effects.fastFirstOrNull { e -> e.index == i }
    } else {
        val n = index.toString()
        layer.effects.fastFirstOrNull { e -> e.name == n || e.matchName == n }
    }
}

internal fun JSLayerToCompOrWorld(
    layer: Layer,
    reverse: Boolean,
    toComp: Boolean,
) = Callable {
    val point = (it[0] as List<JsAny?>).fastMap { toNumber(it) }
    val time = it.getOrNull(1)?.let { toNumber(it) } ?: state.timeSeconds

    if (it.size < 2) {
        convert(layer, point, state, reverse, toComp)
    } else {
        state.onTime(time.toFloat()) {
            convert(layer, point, it, reverse, toComp)
        }
    }
}

private val conversionMatrix = Matrix()

private fun ScriptRuntime.convert(
    layer: Layer,
    point: List<Number>,
    state: AnimationState,
    reverse: Boolean,
    toComp: Boolean,
): JsAny {

    val layerMatrix = layer.totalTransformMatrix(state, toComp = toComp)
    val compMatrix = state.thisComp.transformMatrix(state)

    val offset = Offset(
        point[0].toFloat(),
        point[1].toFloat()
    )

    if (reverse) {
        conversionMatrix.fastSetFrom(layerMatrix)
        compMatrix.invert()
        conversionMatrix.timesAssign(compMatrix)
    } else {
        conversionMatrix.fastSetFrom(compMatrix)
        layerMatrix.invert()
        conversionMatrix.timesAssign(layerMatrix)
    }

    val result = conversionMatrix.map(offset)

    return listOf(result.x.toJs(this), result.y.toJs(this)).toJs(this)
}