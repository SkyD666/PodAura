package io.github.alexzhirkevich.compottie.internal.helpers.text

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
internal value class TextRangeUnits(val type: Byte) {
    companion object {
        val Percent = TextRangeUnits(1)
        val Index = TextRangeUnits(2)
    }
}