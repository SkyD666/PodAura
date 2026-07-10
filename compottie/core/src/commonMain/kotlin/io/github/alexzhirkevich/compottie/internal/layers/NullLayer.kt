package io.github.alexzhirkevich.compottie.internal.layers

import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.effects.LayerEffect
import io.github.alexzhirkevich.compottie.internal.helpers.BooleanIntSerializer
import io.github.alexzhirkevich.compottie.internal.helpers.LottieBlendMode
import io.github.alexzhirkevich.compottie.internal.helpers.Mask
import io.github.alexzhirkevich.compottie.internal.helpers.MatteMode
import io.github.alexzhirkevich.compottie.internal.helpers.Transform
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("3")
internal class NullLayer(
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

    @SerialName("ef")
    override var effects: List<LayerEffect> = emptyList(),
) : BaseLayer() {

    override fun drawLayer(drawScope: DrawScope, parentMatrix: Matrix, parentAlpha: Float, state: AnimationState) {
    }

    override fun getBounds(
        drawScope: DrawScope,
        parentMatrix: Matrix,
        applyParents: Boolean,
        state: AnimationState,
        outBounds: MutableRect
    ) {
        super.getBounds(drawScope, parentMatrix, applyParents, state, outBounds)
        outBounds.set(0f, 0f, 0f, 0f)
    }

    override fun deepCopy(): Layer {
        return NullLayer(
            transform = transform.deepCopy(),
            autoOrient = autoOrient,
            index = index,
            blendMode = blendMode,
            inPoint = inPoint,
            outPoint = outPoint,
            startTime = startTime,
            name = name,
            timeStretch = timeStretch,
            parent = parent,
            matteMode = matteMode,
            matteParent = matteParent,
            matteTarget = matteTarget,
            hidden = hidden,
            masks = masks?.map(Mask::deepCopy),
            hasMask = hasMask,
            effects = effects.map(LayerEffect::copy)
        )
    }
}