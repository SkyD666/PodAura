package com.skyd.podaura.util

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray

fun JsonPrimitive.Companion.from(value: Any?): JsonPrimitive {
    return when (value) {
        is String -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        null -> JsonNull
        else -> throw IllegalArgumentException("Unsupported element type: ${value.let { it::class }}")
    }
}

fun JsonArray.Companion.from(array: List<Any?>): JsonArray {
    return buildJsonArray {
        addAll(array.map { JsonPrimitive.from(it) })
    }
}