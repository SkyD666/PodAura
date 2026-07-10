package io.github.alexzhirkevich.compottie.internal.animation

import androidx.compose.ui.geometry.lerp
import io.github.alexzhirkevich.compottie.internal.AnimationState

internal fun interface VectorKeyframeMapper {
    fun VectorKeyframe.map(start: FloatArray, end: FloatArray, progress: Float): Long
}

internal class VectorKeyframeAnimation(
    index: Int?,
    keyframes: List<VectorKeyframe>,
    private val emptyValue: Vec2,
    private val map: VectorKeyframeMapper
) : BaseKeyframeAnimation<Vec2, FloatArray, VectorKeyframe>(
    index = index,
    sourceKeyframes = keyframes,
    emptyValue = emptyValue,
    map = { s, e, p -> Vec2(map.run { map(s, e, p) }) }
) {
    override fun rawVec(state: AnimationState): Long {

        return tween(
            state = state,
            default = { Vec2(default(it)) },
            fromKeyframe = { Vec2(it[0], it[1]) },
            lerp = ::lerp
        ).packedValue
    }

    private fun default(state: AnimationState): Long {
        val kfId = keyframeNumber(state)
        val range = keyframesMappingRanges[kfId]

        if (range?.first == null || range.second == null)
            return emptyValue.packedValue

        return with(keyframes[kfId.coerceIn(keyframes.indices)]) {
            with(map) {
                map(range.first!!, range.second!!, progress(kfId, state))
            }
        }
    }

    override fun raw(state: AnimationState): Vec2 {
        return Vec2(rawVec(state))
    }
}