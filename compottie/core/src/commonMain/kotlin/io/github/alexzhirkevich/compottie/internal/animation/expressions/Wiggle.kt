package io.github.alexzhirkevich.compottie.internal.animation.expressions

import androidx.compose.ui.geometry.Offset
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.animation.RawProperty
import io.github.alexzhirkevich.compottie.internal.animation.Vec2
import io.github.alexzhirkevich.compottie.internal.timeSeconds
import io.github.alexzhirkevich.keight.Callable
import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random

internal fun JSTemporalWiggle(
    property: RawProperty<*>
): Callable {
    val lastChange: MutableMap<Int, Long> = mutableMapOf()
    val wiggle: MutableMap<Int, Any> = mutableMapOf()
    val prevWiggle: MutableMap<Int, Any> = mutableMapOf()
    return Callable {
        val freq = toNumber(it[0]).toFloat()
        val amp = toNumber(it[1]).toFloat()
        val octaves = it.getOrNull(2)?.let { toNumber(it) }?.toInt() ?: 1
        val ampMult = it.getOrNull(3)?.let { toNumber(it) }?.toFloat() ?: 0.5f
        val t = it.getOrNull(4)

        val time = onTime(t) {
            wiggle(
                value = it.timeSeconds,
                freq = freq,
                amp = amp,
                octaves = octaves,
                ampMult = ampMult,
                state = it,
                lastChange = lastChange,
                wiggle = wiggle,
                prevWiggle = prevWiggle
            )
        }?.toJs(this)

        onTime(time, property::raw).toJs(this)
    }
}

internal fun JSWiggle(
    property: RawProperty<*>
): Callable {
    val lastChange: MutableMap<Int, Long> = mutableMapOf()
    val wiggle: MutableMap<Int, Any> = mutableMapOf()
    val prevWiggle: MutableMap<Int, Any> = mutableMapOf()
    return Callable {
        val freq = toNumber(it[0]).toFloat()
        val amp = toNumber(it[1]).toFloat()
        val octaves = it.getOrNull(2)?.let { toNumber(it) }?.toInt() ?: 1
        val ampMult = it.getOrNull(3)?.let { toNumber(it) }?.toFloat() ?: 0.5f
        val t = it.getOrNull(4)

        onTime(t) {
            wiggle(
                value = property.raw(it),
                freq = freq,
                amp = amp,
                octaves = octaves,
                ampMult = ampMult,
                state = it,
                lastChange = lastChange,
                wiggle = wiggle,
                prevWiggle = prevWiggle
            )?.toJs(this)
        }
    }
}

private fun wiggle(
    value: Any,
    freq: Float,
    amp: Float,
    octaves: Int,
    ampMult: Float,
    state: AnimationState,
    lastChange: MutableMap<Int, Long>,
    wiggle: MutableMap<Int, Any>,
    prevWiggle: MutableMap<Int, Any>
): Any {

    var mValue = value

    repeat(octaves) {
        val octAmp = amp / (if (it == 0) 1f else ampMult.pow(it))
        val octFreq = freq * (if (it == 0) 1f else ampMult.pow(it))

        val octLast = lastChange[it]

        val frameTime = 1000f / octFreq
        val elapsedTime = abs(state.time.inWholeMilliseconds - (octLast ?: 0)).toFloat()

        val progress = if (octLast == null || elapsedTime > frameTime) {
            lastChange[it] = state.time.inWholeMilliseconds

            when (mValue) {
                is Float -> {
                    prevWiggle[it] = (wiggle[it] as? Float) ?: 0f
                    wiggle[it] = -octAmp + Random.nextFloat() * 2 * octAmp
                }

                is Vec2 -> {
                    prevWiggle[it] = (wiggle[it] as? Vec2) ?: Vec2.Zero
                    wiggle[it] = Offset(
                        -octAmp + Random.nextFloat() * 2 * octAmp,
                        -octAmp + Random.nextFloat() * 2 * octAmp,
                    )
                }

                else -> error("Can't wiggle ${mValue::class}")
            }
            0f
        } else {
            (elapsedTime / frameTime).coerceIn(0f, 1f)
        }

        val p = prevWiggle[it]
        val c = wiggle[it]

        when {
            mValue is Float && p is Float && c is Float ->
                mValue += androidx.compose.ui.util.lerp(p, c, progress)

            mValue is Vec2 && p is Vec2 && c is Vec2 ->
                mValue += androidx.compose.ui.geometry.lerp(p, c, progress)

            else -> error("Can't wiggle ${mValue::class}")
        }
    }
    return mValue
}
