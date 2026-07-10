package io.github.alexzhirkevich.compottie.internal.shapes

import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PathEffect
import io.github.alexzhirkevich.compottie.dynamic.DynamicShapeLayerProvider
import io.github.alexzhirkevich.compottie.dynamic.DynamicShapeProvider
import io.github.alexzhirkevich.compottie.dynamic.derive
import io.github.alexzhirkevich.compottie.dynamic.layerPath
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedNumber
import io.github.alexzhirkevich.compottie.internal.content.Content
import io.github.alexzhirkevich.compottie.internal.content.nameOrDefault
import io.github.alexzhirkevich.compottie.internal.utils.appendPathEffect
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("rd")
internal class RoundShape(
    @SerialName("mn")
    override val matchName: String? = null,

    @SerialName("nm")
    override val name: String? = null,

    @SerialName("hd")
    override val hidden: Boolean = false,

    @SerialName("r")
    val radius: AnimatedNumber,
) : Shape {

    @Transient
    private var dynamicShape: DynamicShapeProvider? = null

    fun isHidden(state: AnimationState): Boolean {
        return dynamicShape?.hidden.derive(hidden, state)
    }

    override fun setContents(contentsBefore: List<Content>, contentsAfter: List<Content>) {

    }

    override fun prepareExpressions(state: AnimationState) {
        radius.prepareExpressions(state)
    }

    override fun setDynamicProperties(basePath: String?, properties: DynamicShapeLayerProvider?) {
        super.setDynamicProperties(basePath, properties)
        dynamicShape = properties?.get(layerPath(basePath, nameOrDefault))
    }

    override fun deepCopy(): Shape {
        return RoundShape(
            matchName = matchName,
            name = name,
            hidden = hidden,
            radius = radius.copy()
        )
    }

}

internal fun RoundShape.applyTo(paint: Paint, state: AnimationState) {
    if (!isHidden(state)) {
        val radius = radius.interpolatedFloat(state)
        if (radius > 1) {
            paint.appendPathEffect(PathEffect.cornerPathEffect(radius))
        }
    }
}
