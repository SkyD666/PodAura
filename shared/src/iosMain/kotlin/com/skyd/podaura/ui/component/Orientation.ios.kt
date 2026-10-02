package com.skyd.podaura.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalWindowInfo
import com.skyd.podaura.IosPlayerChrome
import platform.UIKit.UIInterfaceOrientation
import platform.UIKit.UIInterfaceOrientationLandscapeLeft
import platform.UIKit.UIInterfaceOrientationLandscapeRight
import platform.UIKit.UIInterfaceOrientationMask
import platform.UIKit.UIInterfaceOrientationMaskLandscape
import platform.UIKit.UIInterfaceOrientationMaskLandscapeLeft
import platform.UIKit.UIInterfaceOrientationMaskLandscapeRight
import platform.UIKit.UIInterfaceOrientationMaskPortrait
import platform.UIKit.UIInterfaceOrientationMaskPortraitUpsideDown
import platform.UIKit.UIInterfaceOrientationPortrait
import platform.UIKit.UIInterfaceOrientationPortraitUpsideDown

@Composable
actual fun isLandscape(): Boolean {
    val size = LocalWindowInfo.current.containerSize
    return size.width > size.height
}

@Composable
actual fun rememberOrientationController(): OrientationController {
    val controller = remember { IosOrientationController() }
    DisposableEffect(controller) {
        onDispose { controller.unspecified() }
    }
    return controller
}

internal class IosOrientationController(
    private val currentOrientation: () -> UIInterfaceOrientation? = {
        IosPlayerChrome.controller?.view?.window?.windowScene?.interfaceOrientation
    },
    private val requestOrientation: (UIInterfaceOrientationMask) -> Unit = IosPlayerChrome::requestOrientation,
) : OrientationController {
    private var previousOrientation: UIInterfaceOrientationMask? = null

    override fun landscape() {
        // Capture once, before requesting rotation, using the scene rather than the device sensor.
        if (previousOrientation == null) {
            previousOrientation = when (currentOrientation()) {
                UIInterfaceOrientationPortrait -> UIInterfaceOrientationMaskPortrait
                UIInterfaceOrientationPortraitUpsideDown -> UIInterfaceOrientationMaskPortraitUpsideDown
                UIInterfaceOrientationLandscapeLeft -> UIInterfaceOrientationMaskLandscapeLeft
                UIInterfaceOrientationLandscapeRight -> UIInterfaceOrientationMaskLandscapeRight
                else -> return
            }
        }
        requestOrientation(UIInterfaceOrientationMaskLandscape)
    }

    override fun unspecified() {
        val orientation = previousOrientation ?: return
        previousOrientation = null
        requestOrientation(orientation)
    }
}
