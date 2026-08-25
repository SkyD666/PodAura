package com.skyd.podaura.ui.player.coordinator

import com.skyd.podaura.ui.PlatformSurfaceHolder

internal sealed interface PlayerSurfaceEvent {
    data class Created(val holder: PlatformSurfaceHolder) : PlayerSurfaceEvent
    data class Changed(
        val holder: PlatformSurfaceHolder,
        val width: Int,
        val height: Int,
    ) : PlayerSurfaceEvent

    data class Destroyed(val holder: PlatformSurfaceHolder) : PlayerSurfaceEvent
}

internal expect fun onAttach(
    owner: Any,
    surfaceHolder: PlatformSurfaceHolder,
    onEvent: (PlayerSurfaceEvent) -> Unit,
)

internal expect fun onDetach(owner: Any, surfaceHolder: PlatformSurfaceHolder)
internal expect fun onDetachAll(owner: Any)
