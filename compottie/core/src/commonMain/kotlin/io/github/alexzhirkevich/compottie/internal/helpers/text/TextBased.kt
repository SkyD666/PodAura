package io.github.alexzhirkevich.compottie.internal.helpers.text

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
internal value class TextBased(val type: Byte) {
    companion object {
        val Characters = TextBased(1)
        val CharactersExclSpaces = TextBased(2)
        val Words = TextBased(3)
        val Lines = TextBased(4)
    }
}