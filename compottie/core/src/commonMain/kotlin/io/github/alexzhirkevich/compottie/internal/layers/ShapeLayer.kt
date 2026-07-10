package io.github.alexzhirkevich.compottie.internal.layers

import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.util.fastForEach
import io.github.alexzhirkevich.compottie.dynamic.DynamicCompositionProvider
import io.github.alexzhirkevich.compottie.dynamic.DynamicLayerProvider
import io.github.alexzhirkevich.compottie.dynamic.DynamicShapeLayerProvider
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.content.ContentGroup
import io.github.alexzhirkevich.compottie.internal.content.ContentGroupImpl
import io.github.alexzhirkevich.compottie.internal.effects.LayerEffect
import io.github.alexzhirkevich.compottie.internal.helpers.BooleanIntSerializer
import io.github.alexzhirkevich.compottie.internal.helpers.LottieBlendMode
import io.github.alexzhirkevich.compottie.internal.helpers.Mask
import io.github.alexzhirkevich.compottie.internal.helpers.MatteMode
import io.github.alexzhirkevich.compottie.internal.helpers.Transform
import io.github.alexzhirkevich.compottie.internal.shapes.Shape
import io.github.alexzhirkevich.compottie.internal.shapes.TransformShape
import io.github.alexzhirkevich.compottie.internal.utils.firstInstanceOf
import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsAny
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("4")
internal class ShapeLayer(

    @SerialName("ks")
    override val transform: Transform = Transform(),

    @SerialName("ao")
    @Serializable(with = BooleanIntSerializer::class)
    override val autoOrient: Boolean = false,

    @SerialName("ind")
    override val index: Int? = null,

    @SerialName("bm")
    override val blendMode: LottieBlendMode = LottieBlendMode.Normal,

    @SerialName("ip")
    override val inPoint: Float? = null,

    @SerialName("op")
    override val outPoint: Float? = null,

    @SerialName("st")
    override val startTime: Float? = null,

    @SerialName("nm")
    override val name: String? = null,

    @SerialName("ef")
    override var effects: List<LayerEffect> = emptyList(),

    @SerialName("sr")
    override val timeStretch: Float = 1f,

    @SerialName("parent")
    override val parent: Int? = null,

    @SerialName("tt")
    override val matteMode: MatteMode? = null,

    @SerialName("tp")
    override val matteParent: Int? = null,

    @SerialName("td") @Serializable(with = BooleanIntSerializer::class)
    override val matteTarget: Boolean? = null,

    @SerialName("hd")
    override val hidden: Boolean = false,

    @SerialName("masksProperties")
    override val masks: List<Mask>? = null,

    @SerialName("hasMask")
    override val hasMask: Boolean? = null,

    @SerialName("shapes")
    val shapes: List<Shape> = emptyList(),
) : BaseLayer() {

    internal val shapesByName by lazy {
        shapes.associateBy(Shape::name)
    }

    @Transient
    private val contentGroup: ContentGroup = ContentGroupImpl(
        name = CONTAINER_NAME,
        hidden = null, // will be managed by BaseLayer
        contents = shapes,
        transform = shapes.firstInstanceOf<TransformShape>()
    ).apply {
        setContents(emptyList(), emptyList())
    }

    override fun setDynamicProperties(
        composition: DynamicCompositionProvider?,
        state: AnimationState
    ): DynamicLayerProvider? {
        val layer = super.setDynamicProperties(composition, state)
        shapes.fastForEach {
            it.setDynamicProperties(null, layer as? DynamicShapeLayerProvider)
        }
        return layer
    }

    override fun drawLayer(
        drawScope: DrawScope,
        parentMatrix: Matrix,
        parentAlpha: Float,
        state: AnimationState
    ) {
        contentGroup.draw(drawScope, parentMatrix, parentAlpha, state)
    }

    override fun getBounds(
        drawScope: DrawScope,
        parentMatrix: Matrix,
        applyParents: Boolean,
        state: AnimationState,
        outBounds: MutableRect,
    ) {
        super.getBounds(drawScope, parentMatrix, applyParents, state, outBounds)
        contentGroup.getBounds(drawScope, boundsMatrix, applyParents, state, outBounds)
    }

    override fun deepCopy(): Layer {
        return ShapeLayer(
            transform = transform.deepCopy(),
            autoOrient = autoOrient,
            index = index,
            blendMode = blendMode,
            inPoint = inPoint,
            outPoint = outPoint,
            startTime = startTime,
            name = name,
            effects = effects.map(LayerEffect::copy),
            timeStretch = timeStretch,
            parent = parent,
            matteMode = matteMode,
            matteParent = matteParent,
            matteTarget = matteTarget,
            hidden = hidden,
            masks = masks?.map(Mask::deepCopy),
            hasMask = hasMask,
            shapes = shapes.map(Shape::deepCopy)
        )
    }

    override fun prepareExpressions(state: AnimationState) {
        super.prepareExpressions(state)
        shapes.fastForEach { it.prepareExpressions(state) }
    }

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
        return when (property?.toString()) {
            "content" -> Callable {
                val name = it[0]?.toKotlin(this)?.toString() ?: return@Callable null
                shapesByName[name]
            }

            else -> super.get(property, runtime)
        }
    }
}
