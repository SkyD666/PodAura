package io.github.alexzhirkevich.compottie.internal.helpers.text

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
internal value class TextGrouping(val type: Int) {
    companion object {
        val Characters = TextGrouping(1)
        val Word = TextGrouping(2)
        val Line = TextGrouping(3)
        val All = TextGrouping(4)
    }
}