package com.skyd.podaura.ui.player.coordinator

import android.view.SurfaceHolder
import com.skyd.podaura.ui.PlatformSurfaceHolder
import com.skyd.podaura.ui.player.mpv.MPVPlayer
import com.skyd.podaura.ui.player.mpv.surfaceCallback

private var surfaceCallback: SurfaceHolder.Callback? = null

internal actual fun onAttach(surfaceHolder: PlatformSurfaceHolder) {
    surfaceCallback = MPVPlayer.instance.surfaceCallback()
    surfaceHolder.addCallback(surfaceCallback)
}

internal actual fun onDetach(surfaceHolder: PlatformSurfaceHolder) {
    surfaceHolder.removeCallback(surfaceCallback)
    surfaceCallback = null
}