package io.github.alexzhirkevich.compottie.internal.assets

import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.helpers.Transform
import io.github.alexzhirkevich.compottie.internal.layers.PrecompositionLayer
import io.github.alexzhirkevich.compottie.internal.shapes.GroupShape
import io.github.alexzhirkevich.compottie.internal.shapes.Shape
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

@Serializable(with = CharacterPathSerializer::class)
internal sealed interface CharacterPath {

    fun copy(): CharacterPath

    fun draw(
        scope: DrawScope,
        state: AnimationState,
        parentMatrix: Matrix,
        strokePaint: Paint,
        fillPaint: Paint
    )

    @Serializable
    class Shapes(
        @SerialName("shapes")
        val shapes: List<Shape>
    ) : CharacterPath {

        private val shape = GroupShape(items = shapes)

        override fun draw(
            scope: DrawScope,
            state: AnimationState,
            parentMatrix: Matrix,
            strokePaint: Paint,
            fillPaint: Paint
        ) {
            val path = shape.getPath(state)
            path.transform(parentMatrix)

            scope.drawIntoCanvas {
                it.drawPath(path, fillPaint)
                if (strokePaint.strokeWidth > 0f) {
                    it.drawPath(path, strokePaint)
                }
            }
        }

        override fun copy(): CharacterPath {
            return Shapes(shapes.map(Shape::deepCopy))
        }
    }

    @Serializable
    class Precomp(
        @SerialName("refId")
        val refId: String? = null,

        @SerialName("tr")
        val transform: Transform = Transform(),

        @SerialName("ip")
        val inPoint: Float? = null,

        @SerialName("op")
        val outPoint: Float? = null,

        @SerialName("sr")
        val timeStretch: Float = 1f,
    ) : CharacterPath {

        private val layer = if (refId != null) {
            PrecompositionLayer(
                refId = refId,
                transform = transform,
                inPoint = inPoint,
                outPoint = outPoint,
                timeStretch = timeStretch,
                width = 0f,
                height = 0f
            )
        } else null

        override fun copy(): CharacterPath {
            return Precomp(
                refId = refId,
                transform = transform,
                inPoint = inPoint,
                outPoint = outPoint,
                timeStretch = timeStretch
            )
        }

        override fun draw(
            scope: DrawScope,
            state: AnimationState,
            parentMatrix: Matrix,
            strokePaint: Paint,
            fillPaint: Paint
        ) {
            layer?.draw(scope, parentMatrix, 1f, state)
        }
    }
}

internal class CharacterPathSerializer : JsonContentPolymorphicSerializer<CharacterPath>(CharacterPath::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<CharacterPath> {
        return if (element.jsonObject.containsKey("shapes")) {
            CharacterPath.Shapes.serializer()
        } else {
            CharacterPath.Precomp.serializer()
        }
    }
}