package io.github.alexzhirkevich.compottie.internal.animation

import androidx.collection.IntObjectMap
import androidx.collection.MutableIntObjectMap
import androidx.compose.ui.util.fastForEach
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.keight.js.JsAny

internal open class BaseKeyframeAnimation<T : Any, K, KF : Keyframe<K>>(
    override val index: Int?,
    sourceKeyframes: List<KF>,
    private val emptyValue: T,
    private val map: KF.(start: K, end: K, progress: Float) -> T
) : RawKeyframeProperty<T, KF> {

    override val jsCache: MutableMap<String, JsAny?> = HashMap()

    override var group: PropertyGroup? = null

    override val keyframes = if (sourceKeyframes.isSorted())
        sourceKeyframes
    else
        sourceKeyframes.sortedBy(Keyframe<*>::time)

    private val timeIntervals = if (keyframes.isNotEmpty()) {
        List(keyframes.lastIndex) {
            FloatRange(keyframes[it].time, keyframes[it + 1].time)
        }
    } else {
        emptyList()
    }

    protected val firstFrame: Float = if (keyframes.isEmpty()) 0f else keyframes.first().time
    protected val lastFrame: Float = if (keyframes.isEmpty()) 0f else keyframes.last().time

    protected val firstFrameSlop: Float = firstFrame - 0.1f
    protected val lastFrameSlop: Float = lastFrame + 0.1f

    protected val keyframesMappingRanges: IntObjectMap<Pair<K?, K?>> =
        MutableIntObjectMap<Pair<K?, K?>>().apply {
            if (keyframes.isEmpty())
                return@apply

            val first = keyframes.first()
            set(
                -1,
                Pair(
                    first.start,
                    first.endHold ?: keyframes.getOrNull(1)?.start
                )
            )

            val last = keyframes.last()
            val preLast = keyframes.getOrNull(keyframes.lastIndex - 1)

            set(
                keyframes.lastIndex,
                Pair(
                    preLast?.start ?: last.start,
                    last.start ?: preLast?.end ?: preLast?.start,
                )
            )

            for (i in 0 until keyframes.lastIndex) {
                set(
                    i,
                    Pair(
                        keyframes[i].start,
                        keyframes[i].endHold
                            ?: keyframes.getOrNull(i + 1)?.start,
                    )
                )
            }
        }

    inline fun tween(
        state: AnimationState,
        default: (AnimationState) -> T,
        fromKeyframe: (K) -> T,
        lerp: (from: T, to: T, progress: Float) -> T,
    ): T {
        return with(state) {

            val from = frame
            val to = tweenTargetFrame
            val progress = tweenProgress
            val startKf = keyframes.firstOrNull()?.start

            if (!isTweenRunning || to == null)
                return default(this)

            when {
                from in firstFrame .. lastFrame && to in firstFrame .. lastFrame ->
                    lerp(
                        default(this),
                        onFrame(to, default),
                        progress
                    )

                from in firstFrame .. lastFrame && startKf != null ->
                    lerp(
                        default(this),
                        fromKeyframe(startKf),
                        progress
                    )

                to in firstFrame .. lastFrame && startKf != null ->
                    lerp(
                        fromKeyframe(startKf),
                        onFrame(to, default),
                        progress
                    )

                else -> default(this)
            }
        }
    }

    protected fun progress(keyframeIndex: Int, state: AnimationState): Float {
        val frame = state.frame
        return when {
            keyframeIndex < 0 -> 0f
            keyframeIndex > timeIntervals.lastIndex -> 1f
            else -> timeIntervals[keyframeIndex].let {
                (frame - it.start) / (it.endInclusive - it.start)
            }
        }
    }

    protected fun keyframeNumber(state: AnimationState): Int {
        val frame = state.frame
        return when {
            keyframes.isEmpty() -> -2
            frame <= firstFrame -> -1
            frame >= lastFrame -> keyframes.lastIndex
            else -> timeIntervals.binarySearch {
                when {
                    frame < it.start -> 1
                    frame > it.endInclusive -> -1
                    else -> 0
                }
            }.also {
                require(it >= 0, InvalidKeyframeError)
            }
        }
    }

    override fun raw(state: AnimationState): T {
        val kfId = keyframeNumber(state)
        val range = keyframesMappingRanges[kfId] ?: return emptyValue

        if (range.first == null || range.second == null)
            return emptyValue

        return with(keyframes[kfId.coerceIn(keyframes.indices)]) {
            map(range.first!!, range.second!!, progress(kfId, state))
        }
    }
}

private fun List<Keyframe<*>>.isSorted(): Boolean {
    var time = Float.MIN_VALUE
    fastForEach {
        if (it.time < time) {
            return false
        }
        time = it.time
    }
    return true
}

private val InvalidKeyframeError = {
    "Invalid keyframe"
}

private class FloatRange(
    val start: Float,
    val endInclusive: Float
)

