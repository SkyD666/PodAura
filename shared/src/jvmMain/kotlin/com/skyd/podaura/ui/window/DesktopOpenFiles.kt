package com.skyd.podaura.ui.window

import com.skyd.fundation.config.appDirectories
import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.platform
import com.skyd.podaura.ui.notification.DesktopArticleNotifications
import com.sun.jna.Native
import com.sun.jna.win32.StdCallLibrary
import io.github.vinceglb.filekit.PlatformFile
import java.awt.Desktop
import java.io.File
import java.nio.file.Path
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

/** Register before application initialization so Launch Services cold-start events are buffered. */
internal class DesktopOpenFiles : AutoCloseable {
    private val pending = Channel<List<PlatformFile>>(Channel.UNLIMITED)
    val requests = pending.receiveAsFlow()
    private var instance: DesktopInstance? = null

    fun start(arguments: Array<String>): Boolean {
        if (platform == Platform.Windows) {
            val applicationPath = System.getProperty("jpackage.app-path")?.let(Path::of)
                ?: Path.of(DesktopOpenFiles::class.java.protectionDomain.codeSource.location.toURI())
            val instance = DesktopInstance(
                directory = Path.of(appDirectories.dataDir, "instances"),
                applicationPath = applicationPath,
                onRequest = ::receiveArguments,
                allowForeground = { pid -> ForegroundApi.instance.AllowSetForegroundWindow(pid.toInt()) },
            ).also { this.instance = it }
            // Resolve relative paths in the sending process, whose working directory may differ.
            return instance.startOrForward(desktopLaunchArguments(arguments))
        }
        register()
        if (arguments.isNotEmpty()) {
            receiveArguments(desktopLaunchArguments(arguments))
        }
        return true
    }

    private fun receiveArguments(arguments: List<String>) {
        val (uris, paths) = arguments.partition { it.startsWith("podaura:", ignoreCase = true) }
        uris.forEach(DesktopArticleNotifications::activate)
        if (paths.isNotEmpty() || uris.isEmpty()) {
            pending.trySend(paths.map { PlatformFile(File(it)) })
        }
    }

    fun register() {
        if (!Desktop.isDesktopSupported()) return
        val desktop = Desktop.getDesktop()
        if (desktop.isSupported(Desktop.Action.APP_OPEN_URI)) {
            desktop.setOpenURIHandler { event ->
                DesktopArticleNotifications.activate(event.uri.toString())
            }
        }
        if (desktop.isSupported(Desktop.Action.APP_OPEN_FILE)) {
            desktop.setOpenFileHandler { event ->
                if (event.files.isNotEmpty()) {
                    pending.trySend(event.files.map(::PlatformFile))
                }
            }
        }
    }

    override fun close() {
        instance?.close()
        pending.close()
    }

    private interface ForegroundApi : StdCallLibrary {
        fun AllowSetForegroundWindow(processId: Int): Boolean

        companion object {
            val instance: ForegroundApi = Native.load("user32", ForegroundApi::class.java)
        }
    }
}

/** Preserve notification URIs when forwarding a second launch to the running Windows process. */
internal fun desktopLaunchArguments(arguments: Array<String>): List<String> = arguments
    .filter { it.isNotBlank() }
    .map { if (it.startsWith("podaura:", ignoreCase = true)) it else File(it).absoluteFile.normalize().path }
