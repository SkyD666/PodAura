package com.skyd.htmlrender.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString


/**
 * Splits the annotated string into a list of annotated substrings based on the specified list of tags.
 * The tags are processed in the order they appear in the list, and the result of each tag processing is used as the input for the next tag processing.
 *
 * @param tags The list of tags to split the annotated string by, in order of priority.
 */
fun AnnotatedString.splitByAnnotation(tags: List<String>): List<AnnotatedString> =
    when (tags.size) {
        0 -> listOf(this)
        1 -> splitByAnnotation(tags.first())
        else -> {
            tags.fold(listOf(this)) { acc, tag ->
                acc.flatMap { annotatedString ->
                    annotatedString.splitByAnnotation(tag)
                }
            }.toList()
        }
    }


/**
 * It is assumed that the annotations are in ascending order.
 * When annotations overlap or cover each other,the following rules are applied:
 * - If an annotation is fully covered by a previous annotation, they are merged into a single annotation.
 * - If an annotation partially overlaps with a previous annotation, the string is split at the start
 *   of the overlapping portion, and each annotation is applied to its respective part.
 *
 * @param tag The tag to split the annotated string by.
 */
fun AnnotatedString.splitByAnnotation(tag: String): List<AnnotatedString> {
    if (isEmpty()) {
        return listOf(this)
    }

    // +1 to including annotation at last
    val ranges = getStringAnnotations(tag, 0, length + 1)

    if (ranges.isEmpty()) {
        return listOf(this)
    }


    return sequence {
        var index = 0
        for (range in ranges) {
            if (index < range.start) {
                yield(subSequence(index, range.start))
            } else if (index >= range.end && range.start != range.end) {
                // If the current range is fully covered by the previous range and not an empty range,
                // skip it to handle the fully overlapping case
                continue
            }
            // Yield the substring for the current range, starting from the maximum of the current index
            // and the range start to handle the partially overlapping case
            yield(subSequence(maxOf(range.start, index), range.end))
            index = range.end
        }

        // Yield the remaining substring after the last range if there is any
        if (index < length) {
            yield(subSequence(index, length))
        }
    }.toList()
}

fun AnnotatedString.sortAnnotations(
    comparator: Comparator<AnnotatedString.Range<String>> = Comparator { o1, o2 ->
        if (o1.start != o2.start) {
            o1.start.compareTo(o2.start)
        } else {
            o1.end.compareTo(o2.end)

        }
    }
): AnnotatedString = buildAnnotatedString {
    append(AnnotatedString(text, spanStyles, paragraphStyles))
    getStringAnnotations(0, length + 1).sortedWith(comparator).forEach {
        addStringAnnotation(it.tag, it.item, it.start, it.end)
    }
}