@file:Suppress("INVISIBLE_REFERENCE", "UNCHECKED_CAST")

package io.github.alexzhirkevich.compottie.expressions

import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.lerp
import io.github.alexzhirkevich.compottie.internal.animation.RawProperty
import io.github.alexzhirkevich.compottie.internal.animation.expressions.state
import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.js
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

internal fun RawProperty<*>.random(runtime: ScriptRuntime): RandomSource =
    jsCache.getOrPut(RandomSource::class.simpleName!!) { RandomSource(runtime) } as RandomSource

internal fun JSRandomNumber(
    isGauss: Boolean = false
) = Callable {
    val random = state.thisProperty?.random(this)
        ?: return@Callable null

    with(random) {
        when (it.size) {
            0 -> if (isGauss) gaussRandom().js else random().js
            1 -> if (isGauss) gaussRandom(it[0]) else random(it[0])
            else -> if (isGauss) gaussRandom(it[0], it[1]) else random(it[0], it[1])
        }
    }
}

internal fun JSSeedRandom() = Callable {
    val random = state.thisProperty?.random(this)
        ?: return@Callable null

    val seed = toNumber(it[0]).toInt()
    random.setSeed(seed, false)
    Undefined
}

internal fun JSNoise() = Callable {
    val random = state.thisProperty?.random(this) ?: return@Callable Undefined
    noise(it[0], random)
}

private suspend fun ScriptRuntime.noise(time: JsAny?, random: RandomSource): JsAny? {
    return when (time) {
        is List<*> -> time.fastMap { noise(it as JsAny?, random) }.js
        else -> random.noise(toNumber(time).toFloat()).js
    }
}

internal class RandomSource(val runtime: ScriptRuntime) : JsAny by Undefined {

    private var randomInstance: Random = Random
    private val noiseMap = mutableMapOf<Int, Float>()

    fun setSeed(seed: Int, timeless: Boolean) {
        randomInstance = Random(seed)
        noiseMap.clear()
    }

    fun random(): Float {
        return randomInstance.nextFloat()
    }

    suspend fun random(max: JsAny?): JsAny? {
        return when (max) {
            is List<*> -> max.fastMap { random(it as JsAny?) }.js
            else -> runtime.mul(randomInstance.nextFloat().js, max)
        }
    }

    suspend fun random(min: JsAny?, max: JsAny?): JsAny? {
        return when {
            min is List<*> && max is List<*> -> List(min(min.size, max.size)) {
                random(min[it] as JsAny?, max[it] as JsAny?)
            }.js

            else -> runtime.sum(
                random(runtime.sub(max, min)),
                min
            )
        }
    }

    fun gaussRandom(): Float {
        val r = sqrt(-2 * ln(random()))
        val alpha = 2 * PI * random()
        return r * cos(alpha).toFloat() + 1
    }

    suspend fun gaussRandom(max: JsAny?): JsAny? {
        return when (max) {
            is List<*> -> {
                buildList(max.size) {
                    while (this.size < max.size) {
                        val r = sqrt(-2 * ln(randomInstance.nextFloat()))
                        val alpha = 2 * PI * randomInstance.nextFloat()

                        add(
                            runtime.mul(
                                (r * cos(alpha).toFloat() + 1).js,
                                max[size] as JsAny?
                            )
                        )
                        add(
                            runtime.mul(
                                (r * sin(alpha).toFloat() + 1).js,
                                max[size] as JsAny?
                            )
                        )
                    }
                    if (this.size > max.size) {
                        removeAt(lastIndex)
                    }
                }.js
            }

            else -> runtime.mul(gaussRandom().js, max)
        }
    }

    suspend fun gaussRandom(min: JsAny?, max: JsAny?): JsAny? {
        return when {
            min is List<*> && max is List<*> -> {

                val cap = min(min.size, max.size)
                min as List<JsAny?>
                max as List<JsAny?>

                buildList(cap) {
                    while (this.size < cap) {
                        val r = sqrt(-2 * ln(randomInstance.nextFloat()))
                        val alpha = 2 * PI * randomInstance.nextFloat()

                        val maxSubMin = runtime.sub(max[size], min[size])

                        add(
                            runtime.sum(
                                runtime.mul(
                                    (r * cos(alpha).toFloat() + 1).js,
                                    maxSubMin
                                ),
                                min[size]
                            )
                        )
                        add(
                            runtime.sum(
                                runtime.mul(
                                    (r * sin(alpha).toFloat() + 1).js,
                                    maxSubMin
                                ),
                                min[size]
                            )
                        )
                    }
                    if (this.size > cap) {
                        removeAt(lastIndex)
                    }
                }.js
            }

            else -> runtime.sum(
                gaussRandom(runtime.sub(max, min)),
                min
            )
        }
    }

    fun noise(t: Float): Float {
        val prevX = t.toInt()
        val nextX = prevX + 1
        val fracX = t - prevX

        val y0 = getOrInitNoise(prevX)
        val y1 = getOrInitNoise(nextX)
        return lerp(y0, y1, fracX)
    }

    private fun getOrInitNoise(x: Int): Float {
        return noiseMap[x] ?: (randomInstance.nextFloat() * 2 - 1).also {
            noiseMap[x] = it
        }
    }
}


