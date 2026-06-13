package com.skyd.podaura.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.useContents
import platform.AppKit.NSApplication
import platform.AppKit.NSSharingServicePicker
import platform.AppKit.currentEvent
import platform.Foundation.NSMakeRect
import platform.Foundation.NSRectEdgeMinY

@Composable
actual fun rememberTextSharing(): TextSharing = remember {
    object : TextSharing {
        override fun share(text: String) {
            val window = NSApplication.sharedApplication.mainWindow ?: NSApplication.sharedApplication.keyWindow ?: return
            val contentView = window.contentView ?: return

            val locationInWindow = NSApplication.sharedApplication.currentEvent?.locationInWindow ?: return

            val picker = NSSharingServicePicker(items = listOf(text))

            val rect = locationInWindow.useContents {
                NSMakeRect(x, y, 1.0, 1.0)
            }

            picker.showRelativeToRect(
                rect,
                ofView = contentView,
                preferredEdge = NSRectEdgeMinY
            )
        }
    }
}
