package io.github.alexzhirkevich.compottie.internal.helpers

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.intOrNull

internal class BooleanIntSerializer : JsonTransformingSerializer<Boolean>(Boolean.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {

        val int = (element as JsonPrimitive).intOrNull

        return if (int != null) {
            JsonPrimitive(int == 1)
        } else {
            element
        }
    }
}