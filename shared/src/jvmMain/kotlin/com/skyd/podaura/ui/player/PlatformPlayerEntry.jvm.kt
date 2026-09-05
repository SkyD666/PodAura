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
    protected actual override fun openAccepted(request: PlayerOpenRequest) {
        when (request) {
            is PlayerOpenRequest.Media -> player.open(request.mode, request.requestId)
            is PlayerOpenRequest.Files -> {
                player.openFiles(request.files, request.requestId)
                windows.openOrActivate(DesktopWindowSpec.Main)
            }

            PlayerOpenRequest.Resume -> if (player.coordinator == null) return
        }
        windows.openOrActivate(DesktopWindowSpec.Player)
    }

    protected actual override fun showTerms() {
        windows.close(DesktopWindowId.Player)
        player.destroy()
        windows.openOrActivate(DesktopWindowSpec.Main)
    }
}
