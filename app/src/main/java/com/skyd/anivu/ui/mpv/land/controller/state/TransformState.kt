package com.skyd.anivu.ui.mpv.land.controller.state

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset

data class TransformState(
    val videoRotate: Float,
    val videoZoom: Float,
    val videoOffset: Offset,
) {
    companion object {
        val initial = TransformState(
            videoRotate = 0f,
            videoZoom = 1f,
            videoOffset = Offset.Zero,
        )
    }

    fun copyIfNecessary(
        videoRotate: Float = this.videoRotate,
        videoZoom: Float = this.videoZoom,
        videoOffset: Offset = this.videoOffset,
    ): TransformState {
        return if (videoRotate != this.videoRotate ||
            videoZoom != this.videoZoom ||
            videoOffset != this.videoOffset
        ) copy(
            videoRotate = videoRotate,
            videoZoom = videoZoom,
            videoOffset = videoOffset,
        ) else this
    }
}

@Immutable
data class TransformStateCallback(
    val onVideoRotate: (Float) -> Unit,
    val onVideoZoom: (Float) -> Unit,
    val onVideoOffset: (Offset) -> Unit,
)