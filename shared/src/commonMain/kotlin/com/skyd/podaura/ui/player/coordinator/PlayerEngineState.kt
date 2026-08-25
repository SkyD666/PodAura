package com.skyd.podaura.ui.player.coordinator

sealed interface PlayerEngineState {
    data object Initializing : PlayerEngineState
    data object AwaitingMedia : PlayerEngineState
    data object LoadingMedia : PlayerEngineState
    data object Ready : PlayerEngineState
    data class Failed(val message: String) : PlayerEngineState
    data object Destroyed : PlayerEngineState
}

val PlayerEngineState.isReady: Boolean
    get() = this is PlayerEngineState.Ready
