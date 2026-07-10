package io.github.alexzhirkevich.compottie.internal.shapes

import io.github.alexzhirkevich.compottie.dynamic.DynamicShapeLayerProvider
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.animation.ExpressionHolder
import io.github.alexzhirkevich.compottie.internal.content.Content
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsAny
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("ty")
internal sealed interface Shape : Content, ExpressionHolder, JsAny {

    val matchName: String?

    val hidden: Boolean

    fun setDynamicProperties(basePath: String?, properties: DynamicShapeLayerProvider?) {}

    @Serializable
    class Unsupported : Shape {
        override val name: String? get() = null

        override val matchName: String? get() = null
        override val hidden: Boolean get() = true
        override fun setContents(contentsBefore: List<Content>, contentsAfter: List<Content>) {}
        override fun prepareExpressions(state: AnimationState) {

        }

        override fun deepCopy(): Shape = Unsupported()
    }

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
        return when (property?.toString()) {
            "size" -> null
            "position" -> null
            "color" -> null
            "path" -> null
            "scale" -> null
            "rotation" -> null
            "rotationX" -> null
            "rotationY" -> null
            "rotationZ" -> null
            "skew" -> null
            "skewAxis" -> null
            "opacity" -> null
            else -> super.get(property, runtime)
        }
    }

    fun deepCopy(): Shape
}

internal const val DIRECTION_REVERSED: Int = 3
