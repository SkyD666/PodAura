package io.github.alexzhirkevich.compottie.internal.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PathEffect
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

internal fun FloatArray.toOffset() = Offset(this[0], this[1])
internal fun FloatArray.toSize() = Size(this[0], this[1])

private val PiDiv180 = PI / 180
private val PiDiv180Float = PiDiv180.toFloat()

internal fun degreeToRadians(degree: Float): Float {
    return degree * PiDiv180Float
}

internal fun degreeToRadians(degree: Double): Double {
    return degree * PiDiv180
}


internal fun radiansToDegree(radians: Float): Float {
    return radians / PiDiv180Float
}

internal fun radiansToDegree(radians: Double): Double {
    return radians / PiDiv180
}


internal fun floorMod(x: Float, y: Float): Int {
    return floorMod(x.toInt(), y.toInt())
}

internal fun floorMod(x: Int, y: Int): Int {
    return x - y * floorDiv(x, y)
}

private fun floorDiv(x: Int, y: Int): Int {
    var r = x / y
    val sameSign = (x xor y) >= 0
    val mod = x % y
    if (!sameSign && mod != 0) {
        r--
    }
    return r
}

internal fun hslToRed(h: Float, s: Float, l: Float): Float = hslToRgbComponent(0, h, s, l)
internal fun hslToGreen(h: Float, s: Float, l: Float): Float = hslToRgbComponent(8, h, s, l)
internal fun hslToBlue(h: Float, s: Float, l: Float): Float = hslToRgbComponent(4, h, s, l)

private fun hslToRgbComponent(n: Int, h: Float, s: Float, l: Float): Float {
    val k = (n.toFloat() + h / 30f) % 12f
    val a = s * min(l, 1f - l)
    return l - a * max(-1f, minOf(k - 3, 9 - k, 1f))
}

internal inline fun <reified R> List<*>.firstInstanceOf(): R? {
    return firstOrNull { it is R } as? R
}

internal inline fun <reified R> List<*>.getAs(index: Int): R? {
    return getOrNull(index) as? R
}

internal fun Paint.appendPathEffect(effect: PathEffect) {
    pathEffect = if (pathEffect == null) {
        effect
    } else {
        PathEffect.chainPathEffect(effect, pathEffect!!)
    }
}
