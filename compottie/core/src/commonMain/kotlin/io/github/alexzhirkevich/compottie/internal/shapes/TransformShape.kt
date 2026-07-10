package io.github.alexzhirkevich.compottie.internal.shapes

import io.github.alexzhirkevich.compottie.dynamic.DynamicShapeLayerProvider
import io.github.alexzhirkevich.compottie.dynamic.DynamicShapeProvider
import io.github.alexzhirkevich.compottie.dynamic.derive
import io.github.alexzhirkevich.compottie.dynamic.layerPath
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedNumber
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedTransform
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedVector2
import io.github.alexzhirkevich.compottie.internal.animation.defaultAnchorPoint
import io.github.alexzhirkevich.compottie.internal.animation.defaultOpacity
import io.github.alexzhirkevich.compottie.internal.animation.defaultPosition
import io.github.alexzhirkevich.compottie.internal.animation.defaultRotation
import io.github.alexzhirkevich.compottie.internal.animation.defaultScale
import io.github.alexzhirkevich.compottie.internal.animation.defaultSkew
import io.github.alexzhirkevich.compottie.internal.animation.defaultSkewAxis
import io.github.alexzhirkevich.compottie.internal.content.Content
import io.github.alexzhirkevich.compottie.internal.content.nameOrDefault
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.Undefined
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("tr")
internal class TransformShape(

    @SerialName("mn")
    override val matchName: String? = null,

    @SerialName("nm")
    override val name: String? = null,

    @SerialName("hd")
    override val hidden: Boolean = false,

    @SerialName("a")
    override val anchorPoint: AnimatedVector2 = AnimatedVector2.defaultAnchorPoint(),

    @SerialName("p")
    override val position: AnimatedVector2 = AnimatedVector2.defaultPosition(),

    @SerialName("s")
    override val scale: AnimatedVector2 = AnimatedVector2.defaultScale(),

    @SerialName("r")
    override val rotation: AnimatedNumber = AnimatedNumber.defaultRotation(),

    @SerialName("rx")
    override val rotationX: AnimatedNumber? = null,

    @SerialName("ry")
    override val rotationY: AnimatedNumber? = null,

    @SerialName("rz")
    override val rotationZ: AnimatedNumber? = null,

    @SerialName("o")
    override val opacity: AnimatedNumber = AnimatedNumber.defaultOpacity(),

    @SerialName("sk")
    override val skew: AnimatedNumber = AnimatedNumber.defaultSkew(),

    @SerialName("sa")
    override val skewAxis: AnimatedNumber = AnimatedNumber.defaultSkewAxis(),
) : AnimatedTransform(), Shape {

    @Transient
    private var dynamicShape: DynamicShapeProvider? = null

    override fun setContents(contentsBefore: List<Content>, contentsAfter: List<Content>) {

    }

    override fun isHidden(state: AnimationState): Boolean {
        return dynamicShape?.hidden.derive(hidden, state)
    }

    override fun setDynamicProperties(basePath: String?, properties: DynamicShapeLayerProvider?) {
        super.setDynamicProperties(basePath, properties)

        dynamicShape = properties?.get(layerPath(basePath, nameOrDefault))
    }

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
        return when (property?.toString()) {
            "position" -> position
            "scale" -> scale
            "rotation" -> rotation
            "rotationX" -> rotationX
            "rotationY" -> rotationY
            "rotationZ" -> rotationZ
            "skew" -> skew
            "skewAxis" -> skewAxis
            "opacity" -> opacity
            else -> {
                super<Shape>.get(property, runtime).let {
                    if (it !is Undefined) return it
                }

                super<AnimatedTransform>.get(property, runtime)
            }
        }
    }

    override fun deepCopy(): Shape {
        return TransformShape(
            matchName = matchName,
            name = name,
            hidden = hidden,
            anchorPoint = anchorPoint.copy(),
            position = position.copy(),
            scale = scale.copy(),
            rotation = rotation.copy(),
            rotationX = rotationX?.copy(),
            rotationY = rotationY?.copy(),
            rotationZ = rotationZ?.copy(),
            opacity = opacity.copy(),
            skew = skew.copy(),
            skewAxis = skewAxis.copy()
        )
    }
}
