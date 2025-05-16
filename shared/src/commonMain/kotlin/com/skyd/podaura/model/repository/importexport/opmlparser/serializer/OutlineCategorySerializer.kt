package com.skyd.podaura.model.repository.importexport.opmlparser.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class OutlineCategorySerializer : KSerializer<List<String>> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("OutlineCategory", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): List<String> {
        return decoder.decodeString().split(",").map { it.trim() }
    }

    override fun serialize(encoder: Encoder, value: List<String>) {
        encoder.encodeString(value.joinToString(","))
    }
}