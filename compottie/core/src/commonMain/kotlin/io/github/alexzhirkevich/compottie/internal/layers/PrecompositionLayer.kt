package io.github.alexzhirkevich.compottie.internal.layers

import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.util.fastMap
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedNumber
import io.github.alexzhirkevich.compottie.internal.animation.expressions.ExpressionComposition
import io.github.alexzhirkevich.compottie.internal.assets.PrecompositionAsset
import io.github.alexzhirkevich.compottie.internal.effects.LayerEffect
import io.github.alexzhirkevich.compottie.internal.helpers.BooleanIntSerializer
import io.github.alexzhirkevich.compottie.internal.helpers.LottieBlendMode
import io.github.alexzhirkevich.compottie.internal.helpers.Mask
import io.github.alexzhirkevich.compottie.internal.helpers.MatteMode
import io.github.alexzhirkevich.compottie.internal.helpers.Transform
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsAny
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SerialName("0")
@Serializable
internal data class PrecompositionLayer(
    val refId: String,

    @SerialName("w")
    override var width: Float,

    @SerialName("h")
    override var height: Float,

    @SerialName("tm")
    override val timeRemapping: AnimatedNumber? = null,

    @SerialName("ind")
    override val index: Int? = null,

    @SerialName("ip")
    override val inPoint: Float? = null,

    @SerialName("op")
    override val outPoint: Float? = null,

    @SerialName("st")
    override val startTime: Float? = null,

    @SerialName("nm")
    override val name: String? = null,

    @SerialName("sr")
    override val timeStretch: Float = 1f,

    @SerialName("parent")
    override val parent: Int? = null,

    @SerialName("hd")
    override val hidden: Boolean = false,

    @SerialName("masksProperties")
    override val masks: List<Mask>? = null,

    @SerialName("hasMask")
    override val hasMask: Boolean? = null,

    @SerialName("ef")
    override var effects: List<LayerEffect> = emptyList(),

    @SerialName("ks")
    override val transform: Transform = Transform(),

    @SerialName("ao")
    @Serializable(with = BooleanIntSerializer::class)
    override val autoOrient: Boolean = false,

    @SerialName("tt")
    override val matteMode: MatteMode? = null,

    @SerialName("tp")
    override val matteParent: Int? = null,

    @SerialName("td") @Serializable(with = BooleanIntSerializer::class)
    override val matteTarget: Boolean? = null,

    @SerialName("bm")
    override val blendMode: LottieBlendMode = LottieBlendMode.Normal,
) : BaseCompositionLayer() {

    private val composition: ExpressionComposition = object : ExpressionComposition {
        override val name: String?
            get() = this@PrecompositionLayer.name
        override val width: Float
            get() = this@PrecompositionLayer.width
        override val height: Float
            get() = this@PrecompositionLayer.height

        override val startTime: Float
            get() {
                val dur = durationFrames
                val t = this@PrecompositionLayer.startTime
                if (dur == 0f || t == null) return 0f
                return t / dur
            }

        override val durationFrames: Float
            get() {
                val ip = inPoint ?: return 0f
                val op = outPoint ?: return 0f

                return op - ip
            }

        override val layersByName: Map<String, Layer> by lazy {
            this@PrecompositionLayer.loadedLayers
                .orEmpty()
                .associateBy { it.name.orEmpty() }
        }

        override val layers: List<Layer>
            get() = loadedLayers.orEmpty()


        override fun transformMatrix(state: AnimationState): Matrix {
            return totalTransformMatrix(state)
        }
    }

    override fun compose(state: AnimationState): List<Layer> {
        return (state.assets[refId] as? PrecompositionAsset?)?.layers
            ?.fastMap(Layer::deepCopy).orEmpty().onEach {
                it.comp = composition
            }
    }

    override fun drawLayer(
        drawScope: DrawScope,
        parentMatrix: Matrix,
        parentAlpha: Float,
        state: AnimationState
    ) {
        state.onComposition(composition) {
            super.drawLayer(drawScope, parentMatrix, parentAlpha, it)
        }
    }

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
        return when (property?.toString()) {
            "timeRemap" -> timeRemapping
            else -> super.get(property, runtime)
        }
    }

    override fun deepCopy(): Layer {
        return PrecompositionLayer(
            refId = refId,
            width = width,
            height = height,
            timeRemapping = timeRemapping,
            index = index,
            inPoint = inPoint,
            outPoint = outPoint,
            startTime = startTime,
            name = name,
            timeStretch = timeStretch,
            parent = parent,
            hidden = hidden,
            masks = masks?.map(Mask::deepCopy),
            hasMask = hasMask,
            effects = effects.map(LayerEffect::copy),
            transform = transform.deepCopy(),
            autoOrient = autoOrient,
            matteMode = matteMode,
            matteParent = matteParent,
            matteTarget = matteTarget,
            blendMode = blendMode,
        )
    }
}