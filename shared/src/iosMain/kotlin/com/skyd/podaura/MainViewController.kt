package com.skyd.podaura

import androidx.compose.ui.window.ComposeUIViewController
import com.skyd.podaura.di.initKoin
import com.skyd.podaura.ui.player.IosPlayerApp
import com.skyd.podaura.ui.player.IosPlayerNavigationController
import com.skyd.podaura.ui.player.IosPlayerSession
import kotlinx.cinterop.cValue
import platform.Foundation.NSOperatingSystemVersion
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIInterfaceOrientationMask
import platform.UIKit.UIViewController
import platform.UIKit.UIWindowSceneGeometryPreferencesIOS
import platform.UIKit.setNeedsUpdateOfHomeIndicatorAutoHidden

internal object IosPlayerChrome {
    var controller: UIViewController? = null
    var fullscreen = false
        private set

    fun setFullscreen(value: Boolean) {
        fullscreen = value
        controller?.setNeedsStatusBarAppearanceUpdate()
        controller?.setNeedsUpdateOfHomeIndicatorAutoHidden()
    }

    fun requestOrientation(orientation: UIInterfaceOrientationMask) {
        if (NSProcessInfo.processInfo.isOperatingSystemAtLeastVersion(
                cValue<NSOperatingSystemVersion> {
                    majorVersion = 16; minorVersion = 0; patchVersion = 0
                }
            )
        ) {
            controller?.view?.window?.windowScene?.requestGeometryUpdateWithPreferences(
                UIWindowSceneGeometryPreferencesIOS(orientation),
                errorHandler = null
            )
        }
    }
}

@Suppress("FunctionName", "unused")
fun MainViewController(): UIViewController {
    initKoin()
    onAppStart()
    val session = IosPlayerSession()
    val root = ComposeUIViewController { IosPlayerApp(session) }
    return IosPlayerNavigationController(root, session::onFullPlayerClosed).also {
        session.navigationController = it
    }
}
