package io.github.alexzhirkevich.compottie.internal.animation.expressions

import io.github.alexzhirkevich.compottie.internal.animation.RawKeyframeProperty
import io.github.alexzhirkevich.keight.Callable
import kotlin.math.abs
import kotlin.math.max

internal fun JsLoopOut(
    isDuration: Boolean
) = Callable {

    val prop = state.thisProperty!!

    if (prop !is RawKeyframeProperty<*, *>) {
        return@Callable prop.raw(state).toJs(this)
    }

    val type = it.getOrNull(0)?.toKotlin(this)?.toString()
    var duration = toNumber(it.getOrNull(1)).toInt()

    val lastKeyFrame = prop.keyframes.last().time

    if (state.frame <= lastKeyFrame) {
        return@Callable prop.raw(state).toJs(this)
    }
    val cycleDuration: Float
    val firstKeyFrame: Float

    if (isDuration) {
        cycleDuration = if (duration == 0) {
            max(0f, lastKeyFrame - (state.thisLayer.inPoint ?: 0f))
        } else {
            abs(lastKeyFrame - state.composition.frameRate * duration)
        }
        firstKeyFrame = lastKeyFrame - cycleDuration
    } else {
        if (duration == 0 || duration > prop.keyframes.lastIndex) {
            duration = prop.keyframes.lastIndex
        }
        firstKeyFrame = prop.keyframes[prop.keyframes.lastIndex - duration].time
        cycleDuration = lastKeyFrame - firstKeyFrame
    }

    when (type?.lowercase()) {
        "pingpong" -> {
            val iterations = ((state.frame - firstKeyFrame) / cycleDuration).toInt()
            if (iterations % 2 == 1) {
                return@Callable state.onFrame(
                    cycleDuration - (state.frame - firstKeyFrame) % cycleDuration + firstKeyFrame,
                    prop::raw
                ).toJs(this)
            }
        }

        "offset" -> {
            val initV = state.onFrame(firstKeyFrame, prop::raw)
            val endV = state.onFrame(lastKeyFrame, prop::raw)
            val current = state.onFrame(
                (state.frame - firstKeyFrame) % cycleDuration + firstKeyFrame,
                prop::raw
            )

            val repeats = ((state.frame - firstKeyFrame) / cycleDuration).toInt()

            return@Callable sum(
                mul(
                    sub(endV.toJs(this), initV.toJs(this)),
                    repeats.toJs(this)
                ),
                current.toJs(this)
            )
        }

        "continue" -> {
            val lastValue = state.onFrame(lastKeyFrame, prop::raw)
            val nextLastValue = state.onFrame(lastKeyFrame - 0.001f, prop::raw)

            val lastJs = lastValue.toJs(this)

            return@Callable mul(
                sum(
                    lastJs,
                    sub(lastJs, nextLastValue.toJs(this))
                ),
                div(
                    sub(state.frame.toJs(this), lastKeyFrame.toJs(this)),
                    0.001.toJs(this)
                )
            )
        }
    }

    state.onFrame(
        (state.frame - firstKeyFrame) % cycleDuration + firstKeyFrame,
        prop::raw
    ).toJs(this)
}

internal fun JSLoopIn(
    isDuration: Boolean,
) = Callable {

    val prop = state.thisProperty!!

    if (prop !is RawKeyframeProperty<*, *>) {
        return@Callable prop.raw(state).toJs(this)
    }

    val type = it.getOrNull(0)?.toKotlin(this)?.toString()
    var duration = toNumber(it.getOrNull(1)).toInt()

    val firstKeyframe = prop.keyframes.first().time

    if (state.frame >= firstKeyframe) {
        return@Callable prop.raw(state).toJs(this)
    }

    val cycleDuration: Float
    val lastKeyFrame: Float

    if (isDuration) {
        cycleDuration = if (duration == 0) {
            max(0f, (state.thisLayer.outPoint ?: 0f) - firstKeyframe)
        } else {
            abs(state.composition.frameRate * duration)
        }
        lastKeyFrame = firstKeyframe + cycleDuration
    } else {
        if (duration == 0 || duration > prop.keyframes.lastIndex) {
            duration = prop.keyframes.lastIndex
        }
        lastKeyFrame = prop.keyframes[duration].time
        cycleDuration = lastKeyFrame - firstKeyframe
    }

    when (type?.lowercase()) {
        "pingpong" -> {
            val iterations = ((firstKeyframe - state.frame) / cycleDuration).toInt()
            if (iterations % 2 == 0) {
                return@Callable state.onFrame(
                    (firstKeyframe - state.frame) % cycleDuration + firstKeyframe,
                    prop::raw
                ).toJs(this)
            }
        }

        "offset" -> {
            val initV = state.onFrame(firstKeyframe, prop::raw)
            val endV = state.onFrame(lastKeyFrame, prop::raw)
            val current = state.onFrame(
                (cycleDuration - ((firstKeyframe - state.frame) % cycleDuration) + firstKeyframe),
                prop::raw
            )

            val repeats = ((firstKeyframe - state.frame) / cycleDuration).toInt() + 1

            return@Callable sub(
                current.toJs(this),
                mul(
                    sub(endV.toJs(this), initV.toJs(this)),
                    repeats.toJs(this)
                )
            )
        }

        "continue" -> {
            val firstValue = state.onFrame(firstKeyframe, prop::raw)
            val nextFirstValue = state.onFrame(firstKeyframe + 0.001f, prop::raw)
            val firstJs = firstValue.toJs(this)
            return@Callable sum(
                firstJs, mul(
                    mul(
                        sub(firstJs, nextFirstValue.toJs(this)),
                        sub(firstJs, state.frame.toJs(this))
                    ),
                    1000.toJs(this)
                )
            )
        }
    }
    state.onFrame(
        cycleDuration - (firstKeyframe - state.frame) % cycleDuration + firstKeyframe,
        prop::raw
    ).toJs(this)
}
