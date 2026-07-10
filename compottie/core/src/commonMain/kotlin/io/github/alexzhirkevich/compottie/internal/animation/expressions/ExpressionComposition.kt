package io.github.alexzhirkevich.compottie.internal.animation.expressions

import androidx.compose.ui.graphics.Matrix
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.layers.Layer
import io.github.alexzhirkevich.compottie.internal.utils.IdentityMatrix
import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsAny

internal interface ExpressionComposition : JsAny {

    val name: String?

    val width: Float

    val height: Float

    val startTime: Float

    val durationFrames: Float

    val layersByName: Map<String, Layer>

    val layers: List<Layer>

    fun transformMatrix(state: AnimationState): Matrix = IdentityMatrix

    override suspend fun keys(
        runtime: ScriptRuntime,
        excludeSymbols: Boolean,
        excludeNonEnumerables: Boolean
    ): List<JsAny?> = listOf(
        "name".toJs(runtime),
        "width".toJs(runtime),
        "height".toJs(runtime),
        "displayStartTime".toJs(runtime),
        "duration".toJs(runtime),
        "frameDuration".toJs(runtime),
        "numLayers".toJs(runtime),
        "layer".toJs(runtime),
    )

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
        return when (property?.toString()) {
            "name" -> name?.toJs(runtime)
            "width" -> width.toJs(runtime)
            "height" -> height.toJs(runtime)
            "displayStartTime" -> startTime.toJs(runtime)
            "duration" -> (durationFrames / runtime.state.composition.frameRate).toJs(runtime)
            "frameDuration" -> (runtime.state.composition.frameRate / 1000).toJs(runtime)
            "numLayers" -> layers.size.toJs(runtime)
            "layer" -> Callable {
                when (val index = it[0]?.toKotlin(this)) {
                    is Number -> layers[index.toInt()]
                    is Layer -> {
                        val idx = layers.indexOf(index)
                        if (idx < 0) {
                            null
                        } else {
                            val relIndex = toNumber(it.getOrNull(1)).toInt()
                            layers[idx + relIndex]
                        }
                    }

                    else -> state.thisComp.layersByName[index.toString()]
                }
            }

            else -> super.get(property, runtime)
        }
    }
}
