package com.skyd.htmlrender.core.util

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.TextUnit

data class ParagraphInterval(
    val start: Int,
    val end: Int,
    val style: ParagraphStyle
) {
    var priority: Int = 0
}

/**
 * Additional line breaks may be introduced
 * @see ParagraphStyle Once a portion of the text is marked with a ParagraphStyle, that portion will be separated from the remaining as if a line feed character was added.
 */
fun List<ParagraphInterval>.buildNotOverlapList(totalLength: Int): List<ParagraphInterval> {
    if (size <= 1) {
        return this
    }

    val indexes = buildSet {
        add(0)
        this@buildNotOverlapList.forEach { styler ->
            add(styler.start)
            add(styler.end)
        }
        add(totalLength)
    }.sorted()


    var start = indexes.firstOrNull() ?: return this

    val sortedList = sortedBy { it.start }

    return buildList {
        indexes.forEach { end ->
            sortedList.filterIncludeList(start, end)?.mergeWithPriority(start, end)?.also(::add)

            start = end
        }
    }
}

internal fun List<ParagraphInterval>.filterIncludeList(
    start: Int,
    end: Int
): List<ParagraphInterval>? = filter { styler ->
    styler.start < end && styler.end > start
}.ifEmpty { null }


internal fun List<ParagraphInterval>.mergeWithPriority(
    start: Int,
    end: Int
): ParagraphInterval? {
    if (isEmpty()) {
        return null
    }
    if (size == 1) {
        return ParagraphInterval(start, end, first().style)
    }

    val sorted = sortedByDescending { it.priority }

    val textAlign = sorted.getFirstValidOrNull(TextAlign.Unspecified) { textAlign }
    val textDirection = sorted.getFirstValidOrNull(TextDirection.Unspecified) { textDirection }
    val lineHeight = sorted.getFirstValidOrNull(TextUnit.Unspecified) { lineHeight }
    val textIndent = sorted.getFirstValidOrNull(null) { textIndent }
    val platformStyle = sorted.getFirstValidOrNull(null) { platformStyle }
    val lineHeightStyle = sorted.getFirstValidOrNull(null) { lineHeightStyle }
    val lineBreak = sorted.getFirstValidOrNull(LineBreak.Unspecified) { lineBreak }
    val hyphens = sorted.getFirstValidOrNull(Hyphens.Unspecified) { hyphens }
    val textMotion = sorted.getFirstValidOrNull(null) { textMotion }


    if (textAlign == null && textDirection == null && lineHeight == null && textIndent == null && platformStyle == null && lineHeightStyle == null && lineBreak == null && hyphens == null && textMotion == null) {
        return null
    }

    return ParagraphInterval(
        start = start,
        end = end,
        style = ParagraphStyle(
            textAlign = textAlign ?: TextAlign.Unspecified,
            textDirection = textDirection ?: TextDirection.Unspecified,
            lineHeight = lineHeight ?: TextUnit.Unspecified,
            textIndent = textIndent,
            platformStyle = platformStyle,
            lineHeightStyle = lineHeightStyle,
            lineBreak = lineBreak ?: LineBreak.Unspecified,
            hyphens = hyphens ?: Hyphens.Unspecified,
            textMotion = textMotion
        )
    )
}

internal inline fun <T> List<ParagraphInterval>.getFirstValidOrNull(
    defaultValue: T,
    map: ParagraphStyle.() -> T
): T? {
    for (e in this) {
        val value = e.style.map()
        if (value != defaultValue) {
            return value
        }
    }
    return null
}