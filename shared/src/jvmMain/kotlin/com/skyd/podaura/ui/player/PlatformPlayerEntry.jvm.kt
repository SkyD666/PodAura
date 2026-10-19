package com.skyd.podaura.ui.player

import com.skyd.podaura.ui.window.DesktopWindowId
import com.skyd.podaura.ui.window.DesktopWindowManager
import com.skyd.podaura.ui.window.DesktopWindowSpec
import com.skyd.podaura.ui.window.PlayerWindowController

actual open class PlatformPlayerEntry internal constructor(
    private val windows: DesktopWindowManager,
    private val player: PlayerWindowController,
    hasAcceptedTerms: (() -> Boolean)? = null,
) : PlayerEntry(hasAcceptedTerms) {
    actual override fun openAccepted(request: PlayerOpenRequest) {
        if (request == PlayerOpenRequest.Resume && player.coordinator == null) return
        player.open(request)
        if (request.showPlayer) {
            if (request is PlayerOpenRequest.Files) windows.openOrActivate(DesktopWindowSpec.Main)
            windows.openOrActivate(DesktopWindowSpec.Player)
        }
    }

    protected actual override fun showTerms() {
        windows.close(DesktopWindowId.Player)
        player.destroy()
        windows.openOrActivate(DesktopWindowSpec.Main)
    }
}
