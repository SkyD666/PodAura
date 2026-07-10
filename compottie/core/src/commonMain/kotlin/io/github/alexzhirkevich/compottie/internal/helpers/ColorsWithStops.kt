package io.github.alexzhirkevich.compottie.internal.helpers

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

public class ColorsWithStops internal constructor(
    size: Int
) {
    internal val colorStops: List<Float> get() = mColorStops
    internal val colors: List<Color> get() = mColors

    private val mColorStops: MutableList<Float> = ArrayList(size)
    private val mColors: MutableList<Color> = ArrayList(size)


    internal fun fill(colors: FloatArray, numberOfColors: Int) {
        resizeTo(numberOfColors)

        repeat(numberOfColors) {
            mColorStops[it] = colors[it * 4]

            mColors[it] = Color(
                red = colors[it * 4 + 1],
                green = colors[it * 4 + 2],
                blue = colors[it * 4 + 3],
            )
        }

        addOpacityStopsToGradientIfNeeded(colors, numberOfColors)
    }


    internal fun interpolateBetween(a: ColorsWithStops, b: ColorsWithStops, progress: Float) {
        val n = minOf(a.colors.size, b.colors.size)

        resizeTo(n)

        repeat(n) { i ->
            mColors[i] = lerp(a.colors[i], b.colors[i], progress)
            mColorStops[i] = androidx.compose.ui.util.lerp(a.colorStops[i], b.colorStops[i], progress)
        }
    }

    private fun resizeTo(size: Int) {
        while (colorStops.size < size) {
            mColorStops.add(0f)
            mColors.add(Color.Transparent)
        }
        while (colorStops.size > size) {
            mColorStops.removeAt(mColorStops.lastIndex)
            mColors.removeAt(mColors.lastIndex)
        }
    }

    private fun addOpacityStopsToGradientIfNeeded(
        array: FloatArray,
        colorPoints: Int,
    ) {
        val startIndex: Int = colorPoints * 4
        if (array.size <= startIndex) {
            return
        }

        // When there are opacity stops, we create a merged list of color stops and opacity stops.
        // For a given color stop, we linearly interpolate the opacity for the two opacity stops around it.
        // For a given opacity stop, we linearly interpolate the color for the two color stops around it.

        val opacityStops = (array.size - startIndex) / 2
        val opacityStopPositions = List(opacityStops) { array[startIndex + 2 * it] }
        val opacityStopOpacities = List(opacityStops) { array[startIndex + 2 * it + 1] }

        // Pre-SKIA (Oreo) devices render artifacts when there is two stops in the same position.
        // As a result, we have to de-dupe the merge color and opacity stop positions.
        val newPositions = mergeUniqueElements(mColorStops, opacityStopPositions)
        val newColorPoints = newPositions.size
        val newColors = MutableList(newColorPoints) { Color.Transparent }

        for (i in 0 until newColorPoints) {
            val position = newPositions[i]
            val colorStopIndex: Int = mColorStops.binarySearch(position)
            var opacityIndex: Int = opacityStopPositions.binarySearch(position)
            if (colorStopIndex < 0 || opacityIndex > 0) {
                // This is a stop derived from an opacity stop.
                if (opacityIndex < 0) {
                    // The formula here is derived from the return value for binarySearch. When an item isn't found, it returns -insertionPoint - 1.
                    opacityIndex = -(opacityIndex + 1)
                }
                newColors[i] = getColorInBetweenColorStops(
                    position,
                    opacityStopOpacities[opacityIndex],
                    mColorStops,
                    mColors
                )
            } else {
                // This os a step derived from a color stop.
                newColors[i] = getColorInBetweenOpacityStops(
                    position,
                    mColors[colorStopIndex],
                    opacityStopPositions,
                    opacityStopOpacities
                )
            }
        }

        resizeTo(newColors.size)

        repeat(newColors.size) {
            mColors[it] = newColors[it]
            mColorStops[it] = newPositions[it]
        }
    }
}


private fun getColorInBetweenColorStops(
    position: Float,
    opacity: Float,
    colorStopPositions: List<Float>,
    colorStopColors: List<Color>
): Color {
    if (colorStopColors.size < 2 || position == colorStopPositions[0]) {
        return colorStopColors[0]
    }
    for (i in 1 until colorStopPositions.size) {
        val colorStopPosition = colorStopPositions[i]
        if (colorStopPosition < position && i != colorStopPositions.size - 1) {
            continue
        }
        if (i == colorStopPositions.size - 1 && position >= colorStopPosition) {
            return Color(
                red = colorStopColors[i].red,
                green = colorStopColors[i].green,
                blue = colorStopColors[i].blue,
                alpha = opacity,
            )
        }
        // We found the position in which position is between i - 1 and i.
        val distanceBetweenColors = colorStopPositions[i] - colorStopPositions[i - 1]
        val distanceToLowerColor = position - colorStopPositions[i - 1]
        val percentage = distanceToLowerColor / distanceBetweenColors

        val upperColor = colorStopColors[i]
        val lowerColor = colorStopColors[i - 1]

        return lerp(lowerColor, upperColor, percentage).copy(alpha = opacity)
    }
    throw IllegalArgumentException("Unreachable code.")
}

private fun getColorInBetweenOpacityStops(
    position: Float,
    color: Color,
    opacityStopPositions: List<Float>,
    opacityStopOpacities: List<Float>
): Color {
    if (opacityStopOpacities.size < 2 || position <= opacityStopPositions[0]) {
        color.copy(alpha = opacityStopOpacities[0])
    }
    for (i in 1 until opacityStopPositions.size) {
        val opacityStopPosition = opacityStopPositions[i]
        if (opacityStopPosition < position && i != opacityStopPositions.size - 1) {
            continue
        }
        val alpha = if (opacityStopPosition <= position) {
            opacityStopOpacities[i]
        } else {
            // We found the position in which position in between i - 1 and i.
            val distanceBetweenOpacities = opacityStopPositions[i] - opacityStopPositions[i - 1]
            val distanceToLowerOpacity = position - opacityStopPositions[i - 1]
            val percentage = distanceToLowerOpacity / distanceBetweenOpacities
            androidx.compose.ui.util.lerp(
                opacityStopOpacities[i - 1],
                opacityStopOpacities[i],
                percentage
            )
        }
        return color.copy(alpha = alpha)
    }
    throw IllegalArgumentException("Unreachable code.")
}

/**
 * Takes two sorted float arrays and merges their elements while removing duplicates.
 */
private fun mergeUniqueElements(arrayA: List<Float>, arrayB: List<Float>): List<Float> {
    if (arrayA.size == 0) {
        return arrayB
    } else if (arrayB.size == 0) {
        return arrayA
    }

    var aIndex = 0
    var bIndex = 0
    var numDuplicates = 0
    // This will be the merged list but may be longer than what is needed if there are duplicates.
    // If there are, the 0 elements at the end need to be truncated.
    val mergedNotTruncated = MutableList(arrayA.size + arrayB.size) { 0f }
    for (i in mergedNotTruncated.indices) {
        val a = if (aIndex < arrayA.size) arrayA[aIndex] else Float.NaN
        val b = if (bIndex < arrayB.size) arrayB[bIndex] else Float.NaN

        if (b.isNaN() || a < b) {
            mergedNotTruncated[i] = a
            aIndex++
        } else if (a.isNaN() || b < a) {
            mergedNotTruncated[i] = b
            bIndex++
        } else {
            mergedNotTruncated[i] = a
            aIndex++
            bIndex++
            numDuplicates++
        }
    }

    if (numDuplicates == 0) {
        return mergedNotTruncated
    }

    return mergedNotTruncated.take(mergedNotTruncated.size - numDuplicates)
}