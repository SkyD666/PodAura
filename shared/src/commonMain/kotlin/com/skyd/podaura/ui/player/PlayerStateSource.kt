package com.skyd.podaura.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skyd.podaura.ui.player.service.PlayerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal data class PlayerProgress(
    val position: Long,
    val duration: Long,
    val buffer: Int,
)

internal data class PlayerTransform(
    val zoom: Float,
    val offsetX: Float,
    val offsetY: Float,
    val rotate: Float,
)

internal fun Flow<PlayerState>.withoutHotPlayerValues(): Flow<PlayerState> = map { state ->
    state.copy(
        position = 0L,
        buffer = 0,
        zoom = 1f,
        offsetX = 0f,
        offsetY = 0f,
        rotate = 0f,
    )
}.distinctUntilChanged()

internal fun Flow<PlayerState>.playerProgressValues(): Flow<PlayerProgress> =
    map { PlayerProgress(it.position, it.duration, it.buffer) }.distinctUntilChanged()

internal fun Flow<PlayerState>.playerTransformValues(): Flow<PlayerTransform> =
    map { PlayerTransform(it.zoom, it.offsetX, it.offsetY, it.rotate) }.distinctUntilChanged()

@Composable
internal fun collectPlayerProgress(source: StateFlow<PlayerState>): State<PlayerProgress> {
    val progress = remember(source) {
        source.playerProgressValues()
    }
    return progress.collectAsStateWithLifecycle(
        initialValue = source.value.let { PlayerProgress(it.position, it.duration, it.buffer) }
    )
}

@Composable
internal fun collectPlayerTransform(source: StateFlow<PlayerState>): State<PlayerTransform> {
    val transform = remember(source) {
        source.playerTransformValues()
    }
    return transform.collectAsStateWithLifecycle(
        initialValue = source.value.let {
            PlayerTransform(it.zoom, it.offsetX, it.offsetY, it.rotate)
        }
    )
}
