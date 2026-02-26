package com.skyd.podaura

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import com.skyd.compone.component.blockString
import com.skyd.podaura.di.initKoin
import com.skyd.podaura.ui.screen.AppEntrance
import com.skyd.podaura.util.ProvidePlatformWindowInsets
import com.skyd.podaura.util.ResourceEnvironmentFix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.AppKit.NSApplication
import platform.AppKit.NSApplicationActivationPolicy
import platform.AppKit.NSApplicationDelegateProtocol
import platform.AppKit.NSWindowStyleMaskFullSizeContentView
import platform.darwin.NSObject
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.app_name

fun main() {
    val nsApplication = NSApplication.sharedApplication()
    nsApplication.setActivationPolicy(NSApplicationActivationPolicy.NSApplicationActivationPolicyRegular)
    nsApplication.delegate = object : NSObject(), NSApplicationDelegateProtocol {
        override fun applicationShouldTerminateAfterLastWindowClosed(sender: NSApplication) = true
    }
    initKoin()
    onAppStart()
    Window(
        title = blockString(Res.string.app_name),
        size = DpSize(1200.dp, 800.dp)
    ) {
        LaunchedEffect(Unit) {
            withContext(Dispatchers.Main) {
                window.titlebarAppearsTransparent = true
                window.styleMask = window.styleMask or NSWindowStyleMaskFullSizeContentView
                window.contentView?.let {
                    it.layer?.setBounds(it.bounds())
                    it.setNeedsDisplay(true)
                }
            }
        }
        ProvidePlatformWindowInsets(
            window = { window },
            content = {
                ResourceEnvironmentFix {
                    AppEntrance()
                }
            }
        )
    }
    nsApplication.run()
}
