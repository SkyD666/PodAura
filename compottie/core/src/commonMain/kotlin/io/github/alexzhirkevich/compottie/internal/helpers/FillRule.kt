package io.github.alexzhirkevich.compottie.internal.helpers

import androidx.compose.ui.graphics.PathFillType
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
internal value class FillRule(val type: Int) {
    companion object {
        val NonZero = FillRule(1)
        val EvenOdd = FillRule(2)
    }
}

internal fun FillRule.asPathFillType(): PathFillType {
    return when (this) {
        FillRule.NonZero -> PathFillType.NonZero
        else -> PathFillType.EvenOdd
    }
}