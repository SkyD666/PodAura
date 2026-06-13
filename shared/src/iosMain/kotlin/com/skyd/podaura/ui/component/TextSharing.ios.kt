package com.skyd.podaura.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIDevice
import platform.UIKit.UIUserInterfaceIdiomPad
import platform.UIKit.popoverPresentationController

@Composable
actual fun rememberTextSharing(): TextSharing {
    val viewController = LocalUIViewController.current
    return remember(viewController) {
        object : TextSharing {
            override fun share(text: String) {
                val activityVC = UIActivityViewController(listOf(text), null)

                if (UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad) {
                    // iPad need sourceView for show
                    activityVC.popoverPresentationController?.apply {
                        sourceView = viewController.view
                        sourceRect = viewController.view.center.useContents { CGRectMake(x, y, 0.0, 0.0) }
                        permittedArrowDirections = 0uL
                    }
                }

                viewController.presentViewController(activityVC, true, null)
            }
        }
    }
}
