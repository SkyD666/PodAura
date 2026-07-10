@file:Suppress("INVISIBLE_REFERENCE", "UNCHECKED_CAST")


package io.github.alexzhirkevich.compottie.expressions

import androidx.compose.ui.util.fastMap
import io.github.alexzhirkevich.compottie.internal.utils.hslToBlue
import io.github.alexzhirkevich.compottie.internal.utils.hslToGreen
import io.github.alexzhirkevich.compottie.internal.utils.hslToRed
import io.github.alexzhirkevich.keight.js.FunctionParam
import io.github.alexzhirkevich.keight.js.JSFunction
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.js

internal fun JSHexToRgb() = JSFunction(FunctionParam("hex")) {
    val str = it[0]?.toString() ?: return@JSFunction Undefined

    str.lowercase()
        .removePrefix("#")
        .removePrefix("0x")
        .padEnd(8, padChar = 'f')
        .take(8)
        .chunked(2)
        .fastMap { (it.toInt(16) / 255f).js }
        .js
}

internal fun JSHslToRgb() = JSFunction(FunctionParam("hsl")) {

    val hsl = it[0]?.toKotlin(this) as List<Number>

    val h = hsl[0].toFloat()
    val s = hsl[1].toFloat()
    val l = hsl[2].toFloat()
    val a = hsl[3].toFloat()

    mutableListOf(
        hslToRed(h, s, l).js,
        hslToGreen(h, s, l).js,
        hslToBlue(h, s, l).js,
        a.js
    ).js
}

internal fun JSRgbToHsl() = JSFunction(FunctionParam("rgb")) {
    val color = it[0]?.toKotlin(this) as List<Number>

    val r = color[0].toFloat()
    val g = color[1].toFloat()
    val b = color[2].toFloat()
    val a = color[3].toFloat()

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    var h: Float
    val s: Float
    val l = (max + min) / 2

    if (max == min) {
        h = 0f // achromatic
        s = 0f // achromatic
    } else {
        val d = max - min
        s = if (l > 0.5) d / (2 - max - min) else d / (max + min)
        h = when (max) {
            r -> (g - b) / d + (if (g < b) 6 else 0)
            g -> (b - r) / d + 2
            b -> (r - g) / d + 4
            else -> error("Should never happend")
        }
        h /= 6
    }

    mutableListOf(h.js, s.js, l.js, a.js).js
}
