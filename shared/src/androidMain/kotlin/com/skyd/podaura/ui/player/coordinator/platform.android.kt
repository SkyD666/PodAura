package com.skyd.podaura.ui.player.coordinator

import android.view.SurfaceHolder
import com.skyd.podaura.ui.PlatformSurfaceHolder
import com.skyd.podaura.ui.player.mpv.MPVPlayer
import com.skyd.podaura.ui.player.mpv.surfaceCallback

// There can be more than one live SurfaceView at a time (e.g. the regular player and the PiP one
// overlap while entering/leaving PiP). A single global slot made the second attach overwrite the
// first callback, so the first one was never removed and leaked the holder.
private val surfaceCallbacks = mutableMapOf<SurfaceHolder, SurfaceHolder.Callback>()

internal actual fun onAttach(surfaceHolder: PlatformSurfaceHolder) {
    // Attaching the same holder twice would register two callbacks; drop the previous one first.
    surfaceCallbacks.remove(surfaceHolder)?.let(surfaceHolder::removeCallback)
    val callback = MPVPlayer.instance.surfaceCallback()
    surfaceCallbacks[surfaceHolder] = callback
    surfaceHolder.addCallback(callback)
}

internal actual fun onDetach(surfaceHolder: PlatformSurfaceHolder) {
    surfaceCallbacks.remove(surfaceHolder)?.let(surfaceHolder::removeCallback)
}
