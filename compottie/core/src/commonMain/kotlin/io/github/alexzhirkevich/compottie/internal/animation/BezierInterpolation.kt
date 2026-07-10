package io.github.alexzhirkevich.compottie.internal.animation

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.FloatArraySerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer

@Serializable
public class BezierInterpolation(
    @Serializable(with = BezierCoordinateSerializer::class)
    public val x: FloatArray,

    @Serializable(with = BezierCoordinateSerializer::class)
    public val y: FloatArray
)

internal class BezierCoordinateSerializer : JsonTransformingSerializer<FloatArray>(
    FloatArraySerializer()
) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return if (element is JsonPrimitive) {
            JsonArray(listOf(element))
        } else element
    }
}

