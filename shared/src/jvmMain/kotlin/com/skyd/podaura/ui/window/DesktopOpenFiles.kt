package com.skyd.podaura.ui.window

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import java.awt.Desktop

/** Register before application initialization so Launch Services cold-start events are buffered. */
internal class DesktopOpenFiles {
    private val pending = Channel<List<PlatformFile>>(Channel.CONFLATED)
    val requests = pending.receiveAsFlow()

    fun register() {
        if (!Desktop.isDesktopSupported()) return
        val desktop = Desktop.getDesktop()
        if (desktop.isSupported(Desktop.Action.APP_OPEN_FILE)) {
            desktop.setOpenFileHandler { event ->
                if (event.files.isNotEmpty()) {
                    pending.trySend(event.files.map(::PlatformFile))
                }
            }
        }
    }
}
