package com.skyd.podaura.ui.player.coordinator

import com.skyd.podaura.ui.PlatformSurfaceHolder

internal expect fun onAttach(surfaceHolder: PlatformSurfaceHolder)
internal expect fun onDetach(surfaceHolder: PlatformSurfaceHolder)