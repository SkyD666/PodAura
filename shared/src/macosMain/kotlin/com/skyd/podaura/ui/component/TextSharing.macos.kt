package com.skyd.podaura.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.AppKit.NSApp
import platform.AppKit.NSSharingServicePicker
import platform.CoreGraphics.CGRectGetMidX
import platform.CoreGraphics.CGRectGetMidY
import platform.Foundation.NSArray
import platform.Foundation.NSMakeRect
import platform.Foundation.NSMinYEdge
import platform.Foundation.arrayWithObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun rememberTextSharing(): TextSharing = remember {
    object : TextSharing {
        override fun share(text: String) = dispatch_async(dispatch_get_main_queue()) {
            val contentView = NSApp?.keyWindow?.contentView ?: return@dispatch_async

            val picker = NSSharingServicePicker(
                items = NSArray.arrayWithObject(text)
            )

            val bounds = contentView.bounds
            val rect = NSMakeRect(
                CGRectGetMidX(bounds),
                CGRectGetMidY(bounds),
                1.0,
                1.0
            )

            picker.showRelativeToRect(
                rect,
                ofView = contentView,
                preferredEdge = NSMinYEdge
            )
        }
    }
}
