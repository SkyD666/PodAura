package io.github.alexzhirkevich.compottie.internal.platform

import androidx.compose.ui.util.fastAny

internal expect fun CharSequence.codePointAt(index: Int): Int

internal expect fun charCount(codePoint: Int): Int

internal expect fun StringBuilder.addCodePoint(codePoint: Int)

private val modifierSet = listOf(
    CharCategory.FORMAT,
    CharCategory.MODIFIER_SYMBOL,
    CharCategory.NON_SPACING_MARK,
    CharCategory.OTHER_SYMBOL,
    CharCategory.SURROGATE,
)

internal fun isModifier(codePoint: Int): Boolean {
    return modifierSet.fastAny { it.contains(codePoint.toChar()) }
}
