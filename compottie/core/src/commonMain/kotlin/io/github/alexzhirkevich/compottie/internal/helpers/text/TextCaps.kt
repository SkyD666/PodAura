package io.github.alexzhirkevich.compottie.internal.helpers.text

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
internal value class TextCaps(val type: Byte) {
    companion object {
        val Regular = TextCaps(0)
        val AllCaps = TextCaps(1)
        val SmallCaps = TextCaps(2)
    }
}