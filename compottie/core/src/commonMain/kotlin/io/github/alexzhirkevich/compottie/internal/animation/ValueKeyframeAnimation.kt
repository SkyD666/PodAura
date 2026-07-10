package io.github.alexzhirkevich.compottie.internal.animation

import androidx.compose.ui.util.lerp
import io.github.alexzhirkevich.compottie.internal.AnimationState

internal fun interface ValueKeyframeMapper {
    fun ValueKeyframe.map(start: FloatArray, end: FloatArray, progress: Float): Float
}

internal class ValueKeyframeAnimation(
    index: Int?,
    keyframes: List<ValueKeyframe>,
    private val emptyValue: Float,
    private val map: ValueKeyframeMapper,
) : BaseKeyframeAnimation<Float, FloatArray, ValueKeyframe>(
    index = index,
    sourceKeyframes = keyframes,
    emptyValue = emptyValue,
    map = { s, e, p -> map.run { map(s, e, p) } }
) {

    override fun rawFloat(state: AnimationState): Float {
        return tween(
            state = state,
            default = ::default,
            fromKeyframe = { it[0] },
            lerp = ::lerp
        )
    }

    @Deprecated("prefer rawFloat without boxing", ReplaceWith("rawFloat(state)"))
    override fun raw(state: AnimationState): Float = rawFloat(state)

    private fun default(state: AnimationState): Float {
        val kfId = keyframeNumber(state)
        val range = keyframesMappingRanges[kfId]

        if (range?.first == null || range.second == null)
            return emptyValue

        return with(keyframes[kfId.coerceIn(keyframes.indices)]) {
            with(map) {
                map(range.first!!, range.second!!, progress(kfId, state))
            }
        }
    }

}