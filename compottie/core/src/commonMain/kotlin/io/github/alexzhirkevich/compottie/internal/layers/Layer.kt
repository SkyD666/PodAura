package io.github.alexzhirkevich.compottie.internal.layers

import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import io.github.alexzhirkevich.compottie.dynamic.DynamicCompositionProvider
import io.github.alexzhirkevich.compottie.dynamic.DynamicLayerProvider
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.animation.ExpressionHolder
import io.github.alexzhirkevich.compottie.internal.animation.expressions.ExpressionComposition
import io.github.alexzhirkevich.compottie.internal.animation.expressions.JSGetLayerEffect
import io.github.alexzhirkevich.compottie.internal.animation.expressions.JSLayerToCompOrWorld
import io.github.alexzhirkevich.compottie.internal.animation.expressions.state
import io.github.alexzhirkevich.compottie.internal.animation.expressions.toJs
import io.github.alexzhirkevich.compottie.internal.content.DrawingContent
import io.github.alexzhirkevich.compottie.internal.effects.LayerEffect
import io.github.alexzhirkevich.compottie.internal.effects.LayerEffectsApplier
import io.github.alexzhirkevich.compottie.internal.helpers.LottieBlendMode
import io.github.alexzhirkevich.compottie.internal.helpers.Mask
import io.github.alexzhirkevich.compottie.internal.helpers.MatteMode
import io.github.alexzhirkevich.compottie.internal.helpers.Transform
import io.github.alexzhirkevich.compottie.internal.utils.preConcat
import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.Undefined
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlin.jvm.JvmInline

@JsonClassDiscriminator("ty")
internal sealed interface Layer : DrawingContent, ExpressionHolder, Callable {

    val hidden: Boolean

    val index: Int?

    val parent: Int?

    val timeStretch: Float

    val inPoint: Float?

    val outPoint: Float?

    val startTime: Float?

    val blendMode: LottieBlendMode

    val transform: Transform

    val autoOrient: Boolean

    val matteMode: MatteMode?

    val matteParent: Int?

    val matteTarget: Boolean?

    val hasMask: Boolean?

    val masks: List<Mask>?

    var effects: List<LayerEffect>

    val effectsApplier: LayerEffectsApplier

    var resolvingPath: ResolvingPath?

    var parentLayer: Layer?

    val jsCache: MutableMap<String, JsAny?>

    val matteLayer: Layer?

    var comp: ExpressionComposition?

    fun setDynamicProperties(
        composition: DynamicCompositionProvider?,
        state: AnimationState
    ): DynamicLayerProvider?

    fun deepCopy(): Layer

    fun isHidden(state: AnimationState): Boolean

    fun isActive(state: AnimationState): Boolean

    override fun prepareExpressions(state: AnimationState) {
        transform.prepareExpressions(state)
        effects.fastForEach { it.prepareExpressions(state) }
        masks?.fastForEach { it.prepareExpressions(state) }
    }

    override suspend fun bind(
        thisArg: JsAny?,
        args: List<JsAny?>,
        runtime: ScriptRuntime
    ): Callable = this

    override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
        return when (args.getOrNull(0)?.toString()) {
            "1" -> if (isActive(runtime.state)) 1.toJs(runtime) else 0.toJs(runtime)
            "6" -> if (this is BaseCompositionLayer) timeRemapping else Undefined
            "7" -> name?.toJs(runtime)
            "8" -> parentLayer
            "9" -> blendMode.type.toJs(runtime)
            "10" -> matteLayer
            "Effects", "ADBE Effect Parade" -> jsCache.getOrPut("Effects") {
                Callable {
                    val name = it.getOrNull(0)?.toString() ?: return@Callable null
                    effects.fastFirstOrNull { it.name == name || it.matchName == name }
                }
            }

            else -> Undefined
        }
    }

    override suspend fun keys(
        runtime: ScriptRuntime,
        excludeSymbols: Boolean,
        excludeNonEnumerables: Boolean
    ): List<JsAny?> = listOf(
        "index".toJs(runtime),
        "name".toJs(runtime),
        "source".toJs(runtime),
        "inPoint".toJs(runtime),
        "outPoint".toJs(runtime),
        "startTime".toJs(runtime),
        "source".toJs(runtime),
        "active".toJs(runtime),
        "enabled".toJs(runtime),
        "hasParent".toJs(runtime),
        "parent".toJs(runtime),
        "transform".toJs(runtime),
        "rotation".toJs(runtime),
        "position".toJs(runtime),
        "opacity".toJs(runtime),
        "timeRemap".toJs(runtime),
        "effect".toJs(runtime),
        "eff".toJs(runtime),
        "toComp".toJs(runtime),
        "fromComp".toJs(runtime),
        "toWorld".toJs(runtime),
        "fromWorld".toJs(runtime),
    )

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
        return when (property?.toString()) {
            "index" -> index?.toJs(runtime) ?: Undefined
            "name" -> name?.toJs(runtime) ?: Undefined
            "source" -> comp
            "inPoint" -> inPoint?.div(runtime.state.composition.frameRate)?.toJs(runtime) ?: Undefined
            "outPoint" -> outPoint?.div(runtime.state.composition.frameRate)?.toJs(runtime) ?: Undefined
            "startTime" -> startTime?.div(runtime.state.composition.frameRate)?.toJs(runtime) ?: Undefined
            "active" -> isActive(runtime.state).toJs(runtime)
            "enabled" -> isHidden(runtime.state).not().toJs(runtime)
            "hasParent" -> (parentLayer != null).toJs(runtime)
            "parent" -> parentLayer ?: Undefined
            "transform" -> transform
            "rotation" -> transform.rotation
            "position" -> transform.position
            "opacity" -> transform.opacity
            "effect", "eff" -> jsCache.getOrPut("effect") { JSGetLayerEffect(this) }
            "mask" -> jsCache.getOrPut("mask") {
                Callable {
                    val name = it[0]?.toKotlin(this)?.toString() ?: return@Callable Undefined
                    masks?.fastFirstOrNull { it.name == name }
                }
            }

            "toComp" -> jsCache.getOrPut("toComp") {
                JSLayerToCompOrWorld(layer = this, reverse = false, toComp = true)
            }

            "fromComp" -> jsCache.getOrPut("fromComp") {
                JSLayerToCompOrWorld(layer = this, reverse = true, toComp = true)
            }

            "toWorld" -> jsCache.getOrPut("toWorld") {
                JSLayerToCompOrWorld(layer = this, reverse = false, toComp = false)
            }

            "fromWorld" -> jsCache.getOrPut("fromWorld") {
                JSLayerToCompOrWorld(layer = this, reverse = true, toComp = false)
            }

            else -> super.get(property, runtime)
        }
    }
}

internal fun Layer.totalTransformMatrix(state: AnimationState, toComp: Boolean = false): Matrix {
    val matrix = transform.matrix(state)
    applyParentsMatrix(matrix, state, toComp)
    return matrix
}

internal fun Layer.applyParentsMatrix(matrix: Matrix, state: AnimationState, toComp: Boolean = false) {

    var layer = parentLayer

    while (layer != null) {
        matrix.preConcat(layer.transform.matrix(state))
        if (toComp && layer is PrecompositionLayer) {
            break
        }
        layer = layer.parentLayer
    }
}

@JvmInline
internal value class ResolvingPath private constructor(val path: String) {
    fun resolve(child: String) = ResolvingPath("$path/$child")

    companion object {
        val root = ResolvingPath("")
    }
}

internal val Layer.isContainerLayer get() = name == CONTAINER_NAME



