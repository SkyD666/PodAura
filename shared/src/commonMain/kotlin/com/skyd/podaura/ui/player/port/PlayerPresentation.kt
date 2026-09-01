package com.skyd.podaura.ui.player.port

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface PlayerPresentationState {
    data object Ready : PlayerPresentationState
    data object Loading : PlayerPresentationState
    data class Failed(val message: String) : PlayerPresentationState
}

internal const val PLAYER_PRESENTATION_CROSSFADE_MILLIS = 150
