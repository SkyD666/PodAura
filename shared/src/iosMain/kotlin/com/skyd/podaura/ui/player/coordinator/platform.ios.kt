package com.skyd.podaura.ui.player.coordinator

import com.skyd.podaura.ui.PlatformSurfaceHolder

internal actual fun onAttach(
    owner: Any,
    surfaceHolder: PlatformSurfaceHolder,
    onEvent: (PlayerSurfaceEvent) -> Unit,
) {
    onEvent(PlayerSurfaceEvent.Created(surfaceHolder))
}

internal actual fun onDetach(owner: Any, surfaceHolder: PlatformSurfaceHolder) {
}

internal actual fun onDetachAll(owner: Any) {
}
