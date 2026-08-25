package com.skyd.podaura.ui.player.coordinator

import android.view.SurfaceHolder
import com.skyd.podaura.ui.PlatformSurfaceHolder

// There can be more than one live SurfaceView at a time (e.g. the regular player and the PiP one
// overlap while entering/leaving PiP). A single global slot made the second attach overwrite the
// first callback, so the first one was never removed and leaked the holder.
private val surfaceCallbacks =
    mutableMapOf<Any, MutableMap<SurfaceHolder, SurfaceHolder.Callback>>()

internal actual fun onAttach(
    owner: Any,
    surfaceHolder: PlatformSurfaceHolder,
    onEvent: (PlayerSurfaceEvent) -> Unit,
) {
    val ownerCallbacks = surfaceCallbacks.getOrPut(owner) { mutableMapOf() }
    // Attaching the same holder twice would register two callbacks; drop the previous one first.
    ownerCallbacks.remove(surfaceHolder)?.let(surfaceHolder::removeCallback)
    val callback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            onEvent(PlayerSurfaceEvent.Created(holder))
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            onEvent(PlayerSurfaceEvent.Changed(holder, width, height))
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            onEvent(PlayerSurfaceEvent.Destroyed(holder))
        }
    }
    ownerCallbacks[surfaceHolder] = callback
    surfaceHolder.addCallback(callback)
}

internal actual fun onDetach(owner: Any, surfaceHolder: PlatformSurfaceHolder) {
    val ownerCallbacks = surfaceCallbacks[owner] ?: return
    ownerCallbacks.remove(surfaceHolder)?.let(surfaceHolder::removeCallback)
    if (ownerCallbacks.isEmpty()) surfaceCallbacks.remove(owner)
}

internal actual fun onDetachAll(owner: Any) {
    surfaceCallbacks.remove(owner)?.forEach { (holder, callback) ->
        holder.removeCallback(callback)
    }
}
