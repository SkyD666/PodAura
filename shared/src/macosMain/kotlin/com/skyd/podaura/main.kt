package com.skyd.podaura

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.skyd.compone.component.blockString
import com.skyd.podaura.di.initKoin
import com.skyd.podaura.ui.component.Window
import com.skyd.podaura.ui.local.LocalMacosMainWindow
import com.skyd.podaura.ui.player.LocalMacosPlayerSession
import com.skyd.podaura.ui.player.LocalPlayerSession
import com.skyd.podaura.ui.player.MacosPlayerSession
import com.skyd.podaura.ui.screen.AppEntrance
import platform.AppKit.NSApplication
import platform.AppKit.NSApplicationActivationPolicy
import platform.AppKit.NSApplicationDelegateProtocol
import platform.AppKit.NSApplicationTerminateReply
import platform.AppKit.NSMenu
import platform.AppKit.NSMenuItem
import platform.AppKit.NSTerminateLater
import platform.AppKit.NSWindow
import platform.Foundation.NSNotification
import platform.darwin.NSObject
import platform.darwin.sel_registerName
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.app_name

// AppKit keeps its delegate weakly. Keep the host alive for the application lifetime.
private var applicationDelegate: NSApplicationDelegateProtocol? = null

fun main() {
    val nsApplication = NSApplication.sharedApplication()
    nsApplication.setActivationPolicy(NSApplicationActivationPolicy.NSApplicationActivationPolicyRegular)
    initKoin()
    onAppStart()
    var mainWindow: NSWindow? = null
    val session = MacosPlayerSession(mainWindow = { mainWindow })
    applicationDelegate = object : NSObject(), NSApplicationDelegateProtocol {
        override fun applicationShouldTerminateAfterLastWindowClosed(sender: NSApplication) = false
        override fun applicationShouldTerminate(sender: NSApplication): NSApplicationTerminateReply {
            session.shutdown { sender.replyToApplicationShouldTerminate(true) }
            return NSTerminateLater
        }

        override fun applicationDidFinishLaunching(notification: NSNotification) {
            val host = Window(
                title = blockString(Res.string.app_name),
                size = DpSize(1200.dp, 800.dp),
                onClose = {
                    mainWindow = null
                    nsApplication.terminate(null)
                }
            ) {
                CompositionLocalProvider(
                    LocalMacosMainWindow provides window,
                    LocalPlayerSession provides session,
                    LocalMacosPlayerSession provides session,
                ) { AppEntrance() }
            }
            mainWindow = host.window
            val menu = NSMenu()
            val applicationMenu = NSMenu()
            val applicationItem = NSMenuItem()
            applicationItem.submenu = applicationMenu
            menu.addItem(applicationItem)
            applicationMenu.addItem(
                NSMenuItem(
                    "Quit PodAura",
                    sel_registerName("terminate:"),
                    "q"
                ).apply {
                    target = nsApplication
                })
            nsApplication.mainMenu = menu
        }
    }
    nsApplication.delegate = applicationDelegate
    nsApplication.run()
    applicationDelegate = null
    mainWindow = null
}
