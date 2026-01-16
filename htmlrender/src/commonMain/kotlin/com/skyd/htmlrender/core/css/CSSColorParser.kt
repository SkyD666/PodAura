package com.skyd.htmlrender.core.css

import androidx.compose.ui.graphics.Color
import co.touchlab.kermit.Logger
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt

private const val MODULE = "CSSColorParser-compose"

class CSSColorParser {
    private val blankRegex by lazy { "\\s+".toRegex() }

    fun parseColor(cssColor: String): Color? = when {
        cssColor.startsWith("#") -> {
            runCatching {
                val cleanHex = cssColor.removePrefix("#")
                when (cleanHex.length) {
                    3, 4 -> {
                        //#RGB or #RGBA
                        val r = cleanHex[0].toString().repeat(2)
                        val g = cleanHex[1].toString().repeat(2)
                        val b = cleanHex[2].toString().repeat(2)
                        val a = if (cleanHex.length == 4) cleanHex[3].toString().repeat(2) else "FF"
                        parseHexColor(r, g, b, a)
                    }

                    6, 8 -> {
                        // #RRGGBB or #RRGGBBAA
                        val r = cleanHex.substring(0, 2)
                        val g = cleanHex.substring(2, 4)
                        val b = cleanHex.substring(4, 6)
                        val a = if (cleanHex.length == 8) cleanHex.substring(6, 8) else "FF"
                        parseHexColor(r, g, b, a)
                    }

                    else -> throw IllegalArgumentException("Illegal length")
                }
            }.onFailure {
                Logger.e(MODULE, it) {
                    "Failed to parse the hex color: $cssColor"
                }
            }.getOrNull()
        }

        cssColor.startsWith("rgb") || cssColor.startsWith("rgba") -> {
            runCatching {
                val rgba = splitColorValue(cssColor)
                val r = rgba[0].toFloat().roundToInt()
                val g = rgba[1].toFloat().roundToInt()
                val b = rgba[2].toFloat().roundToInt()
                val a = if (rgba.size == 4) {
                    (parseAlpha(rgba[3]) * 0xFF).toInt()
                } else {
                    0xFF
                }
                Color(r, g, b, a)
            }.onFailure {
                Logger.e(MODULE, it) {
                    "Failed to parse the rgba color: $cssColor"
                }
            }.getOrNull()
        }

        cssColor.startsWith("hsl") || cssColor.startsWith("hsla") -> {
            runCatching {
                val hsla = splitColorValue(cssColor)
                val h = parseHue(hsla[0])
                val s = hsla[1].removeSuffix("%").toFloat() / 100
                val l = hsla[2].removeSuffix("%").toFloat() / 100
                val a = if (hsla.size == 4) parseAlpha(hsla[3]) else 1f
                Color.hsl(h, s, l, a)
            }.onFailure {
                Logger.e(MODULE, it) {
                    "Failed to parse the hsla color: $cssColor"
                }
            }.getOrNull()
        }

        cssColor.startsWith("hwb") -> {
            runCatching {
                val hwba = splitColorValue(cssColor)
                val h = parseHue(hwba[0])
                val w = hwba[1].removeSuffix("%").toFloat() / 100
                val b = hwba[2].removeSuffix("%").toFloat() / 100
                val a = if (hwba.size == 4) parseAlpha(hwba[3]) else 1f
                parseHWBColor(h, w, b, a)
            }.onFailure {
                Logger.e(MODULE, it) {
                    "Failed to parse the hwb color: $cssColor"
                }
            }.getOrNull()
        }

        else -> parseColorName(cssColor)
    }

    private fun splitColorValue(cssColor: String) = cssColor.substringAfter("(")
        .substringBefore(")")
        .replace(",", " ")
        .replace("/", " ")
        .split(blankRegex)

    private fun parseHue(hue: String): Float = when {
        hue.endsWith("deg") -> hue.removeSuffix("deg").toFloat()
        hue.endsWith("rad") -> toDegrees(hue.removeSuffix("rad").toDouble()).toFloat()
        hue.endsWith("turn") -> hue.removeSuffix("turn").toFloat() * 360
        else -> hue.toFloat()
    }

    private fun toDegrees(angrad: Double): Double = angrad * 180.0 / PI


    private fun parseAlpha(alpha: String): Float =
        if (alpha.endsWith("%")) {
            alpha.removeSuffix("%").toFloat() / 100
        } else {
            alpha.toFloat()
        }


    private fun parseHexColor(r: String, g: String, b: String, a: String) =
        Color(
            r.toInt(16),
            g.toInt(16),
            b.toInt(16),
            a.toInt(16),
        )


    private fun parseHWBColor(
        hub: Float,
        white: Float,
        black: Float,
        alpha: Float
    ): Color {
        val sum = white + black
        var w = white
        var b = black
        if (sum > 1) {
            val scale = 1 / sum
            w *= scale
            b *= scale
        }
        val l = (1 - black) / 2
        val s = if (l == 0f || l == 1f) 0f else (1 - w - b) / (1 - abs(2 * l - 1))
        return Color.hsl(hub, s, l, alpha)
    }


    private fun parseColorName(cssColor: String) = when (cssColor.trim().lowercase()) {
        "aliceblue" -> Color(240, 248, 255)
        "antiquewhite" -> Color(250, 235, 215)
        "aqua" -> Color(0, 255, 255)
        "aquamarine" -> Color(127, 255, 212)
        "azure" -> Color(240, 255, 255)
        "beige" -> Color(245, 245, 220)
        "bisque" -> Color(255, 228, 196)
        "black" -> Color(0, 0, 0)
        "blanchedalmond" -> Color(255, 235, 205)
        "blue" -> Color(0, 0, 255)
        "blueviolet" -> Color(138, 43, 226)
        "brown" -> Color(165, 42, 42)
        "burlywood" -> Color(222, 184, 135)
        "cadetblue" -> Color(95, 158, 160)
        "chartreuse" -> Color(127, 255, 0)
        "chocolate" -> Color(210, 105, 30)
        "coral" -> Color(255, 127, 80)
        "cornflowerblue" -> Color(100, 149, 237)
        "cornsilk" -> Color(255, 248, 220)
        "crimson" -> Color(220, 20, 60)
        "cyan" -> Color(0, 255, 255)
        "darkblue" -> Color(0, 0, 139)
        "darkcyan" -> Color(0, 139, 139)
        "darkgoldenrod" -> Color(184, 134, 11)
        "darkgray" -> Color(169, 169, 169)
        "darkgreen" -> Color(0, 100, 0)
        "darkkhaki" -> Color(189, 183, 107)
        "darkmagenta" -> Color(139, 0, 139)
        "darkolivegreen" -> Color(85, 107, 47)
        "darkorange" -> Color(255, 140, 0)
        "darkorchid" -> Color(153, 50, 204)
        "darkred" -> Color(139, 0, 0)
        "darksalmon" -> Color(233, 150, 122)
        "darkseagreen" -> Color(143, 188, 143)
        "darkslateblue" -> Color(72, 61, 139)
        "darkslategray" -> Color(47, 79, 79)
        "darkturquoise" -> Color(0, 206, 209)
        "darkviolet" -> Color(148, 0, 211)
        "deeppink" -> Color(255, 20, 147)
        "deepskyblue" -> Color(0, 191, 255)
        "dimgray" -> Color(105, 105, 105)
        "dimgrey" -> Color(105, 105, 105)
        "dodgerblue" -> Color(30, 144, 255)
        "firebrick" -> Color(178, 34, 34)
        "floralwhite" -> Color(255, 250, 240)
        "forestgreen" -> Color(34, 139, 34)
        "fuchsia" -> Color(255, 0, 255)
        "gainsboro" -> Color(220, 220, 220)
        "ghostwhite" -> Color(248, 248, 255)
        "gold" -> Color(255, 215, 0)
        "goldenrod" -> Color(218, 165, 32)
        "gray" -> Color(128, 128, 128)
        "green" -> Color(0, 128, 0)
        "greenyellow" -> Color(173, 255, 47)
        "honeydew" -> Color(240, 255, 240)
        "hotpink" -> Color(255, 105, 180)
        "indianred" -> Color(205, 92, 92)
        "indigo" -> Color(75, 0, 130)
        "ivory" -> Color(255, 255, 240)
        "khaki" -> Color(240, 230, 140)
        "lavender" -> Color(230, 230, 250)
        "lavenderblush" -> Color(255, 240, 245)
        "lawngreen" -> Color(124, 252, 0)
        "lemonchiffon" -> Color(255, 250, 205)
        "lightblue" -> Color(173, 216, 230)
        "lightcoral" -> Color(240, 128, 128)
        "lightcyan" -> Color(224, 255, 255)
        "lightgoldenrodyellow" -> Color(250, 250, 210)
        "lightgray" -> Color(211, 211, 211)
        "lightgreen" -> Color(144, 238, 144)
        "lightpink" -> Color(255, 182, 193)
        "lightsalmon" -> Color(255, 160, 122)
        "lightseagreen" -> Color(32, 178, 170)
        "lightskyblue" -> Color(135, 206, 250)
        "lightslategray" -> Color(119, 136, 153)
        "lightsteelblue" -> Color(176, 196, 222)
        "lightyellow" -> Color(255, 255, 224)
        "lime" -> Color(0, 255, 0)
        "limegreen" -> Color(50, 205, 50)
        "linen" -> Color(250, 240, 230)
        "magenta" -> Color(255, 0, 255)
        "maroon" -> Color(128, 0, 0)
        "mediumaquamarine" -> Color(102, 205, 170)
        "mediumblue" -> Color(0, 0, 205)
        "mediumorchid" -> Color(186, 85, 211)
        "mediumpurple" -> Color(147, 112, 219)
        "mediumseagreen" -> Color(60, 179, 113)
        "mediumslateblue" -> Color(123, 104, 238)
        "mediumspringgreen" -> Color(0, 250, 154)
        "mediumturquoise" -> Color(72, 209, 204)
        "mediumvioletred" -> Color(199, 21, 133)
        "midnightblue" -> Color(25, 25, 112)
        "mintcream" -> Color(245, 255, 250)
        "mistyrose" -> Color(255, 228, 225)
        "moccasin" -> Color(255, 228, 181)
        "navajowhite" -> Color(255, 222, 173)
        "navy" -> Color(0, 0, 128)
        "oldlace" -> Color(253, 245, 230)
        "olive" -> Color(128, 128, 0)
        "olivedrab" -> Color(107, 142, 35)
        "orange" -> Color(255, 165, 0)
        "orangered" -> Color(255, 69, 0)
        "orchid" -> Color(218, 112, 214)
        "palegoldenrod" -> Color(238, 232, 170)
        "palegreen" -> Color(152, 251, 152)
        "paleturquoise" -> Color(175, 238, 238)
        "palevioletred" -> Color(219, 112, 147)
        "papayawhip" -> Color(255, 239, 213)
        "peachpuff" -> Color(255, 218, 185)
        "peru" -> Color(205, 133, 63)
        "pink" -> Color(255, 192, 203)
        "plum" -> Color(221, 160, 221)
        "powderblue" -> Color(176, 224, 230)
        "purple" -> Color(128, 0, 128)
        "red" -> Color(255, 0, 0)
        "rosybrown" -> Color(188, 143, 143)
        "royalblue" -> Color(65, 105, 225)
        "saddlebrown" -> Color(139, 69, 19)
        "salmon" -> Color(250, 128, 114)
        "sandybrown" -> Color(244, 164, 96)
        "seagreen" -> Color(46, 139, 87)
        "seashell" -> Color(255, 245, 238)
        "sienna" -> Color(160, 82, 45)
        "silver" -> Color(192, 192, 192)
        "skyblue" -> Color(135, 206, 235)
        "slateblue" -> Color(106, 90, 205)
        "slategray" -> Color(112, 128, 144)
        "snow" -> Color(255, 250, 250)
        "springgreen" -> Color(0, 255, 127)
        "steelblue" -> Color(70, 130, 180)
        "tan" -> Color(210, 180, 140)
        "teal" -> Color(0, 128, 128)
        "thistle" -> Color(216, 191, 216)
        "tomato" -> Color(255, 99, 71)
        "turquoise" -> Color(64, 224, 208)
        "violet" -> Color(238, 130, 238)
        "wheat" -> Color(245, 222, 179)
        "white" -> Color(255, 255, 255)
        "whitesmoke" -> Color(245, 245, 245)
        "yellow" -> Color(255, 255, 0)
        "yellowgreen" -> Color(154, 205, 50)
        else -> null
    }
}