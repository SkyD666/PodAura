package com.skyd.podaura.model.repository.importexport.opmlparser.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class ExpansionStateSerializer : KSerializer<List<Int>> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ExpansionState", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): List<Int> {
        return decoder.decodeString().split(",").map { it.trim().toInt() }
    }

    override fun serialize(encoder: Encoder, value: List<Int>) {
        encoder.encodeString(value.joinToString(","))
    }
}