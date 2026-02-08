package com.skyd.podaura.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.popoverPresentationController
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun rememberTextSharing(): TextSharing = remember {
    object : TextSharing {
        override fun share(text: String) {
            val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
            val activityVC = UIActivityViewController(listOf(text), null)

            activityVC.popoverPresentationController?.apply {
                sourceView = root.view
                sourceRect = root.view.bounds
            }

            dispatch_async(dispatch_get_main_queue()) {
                root.presentViewController(activityVC, true, null)
            }
        }
    }
}
