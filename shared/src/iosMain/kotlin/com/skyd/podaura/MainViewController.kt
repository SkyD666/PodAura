package com.skyd.podaura

import androidx.compose.ui.window.ComposeUIViewController
import com.skyd.podaura.di.initKoin
import com.skyd.podaura.ui.player.IosPlayerApp
import kotlinx.cinterop.cValue
import platform.Foundation.NSOperatingSystemVersion
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIInterfaceOrientationMask
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.UIKit.UIViewController
import platform.UIKit.UIWindowSceneGeometryPreferencesIOS
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController
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
    val compose = ComposeUIViewController { IosPlayerApp() }
    return object : UIViewController(nibName = null, bundle = null) {
        override fun viewDidLoad() {
            super.viewDidLoad()
            IosPlayerChrome.controller = this
            addChildViewController(compose)
            compose.view.setFrame(view.bounds)
            compose.view.autoresizingMask =
                UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
            view.addSubview(compose.view)
            compose.didMoveToParentViewController(this)
        }

        override fun prefersStatusBarHidden(): Boolean = IosPlayerChrome.fullscreen
    }
}
