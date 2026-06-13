package com.skyd.podaura.model.repository

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.AppKit.NSApplication
import platform.AppKit.NSSharingServicePicker
import platform.AppKit.currentEvent
import platform.Foundation.NSMakeRect
import platform.Foundation.NSRectEdgeMinY

actual suspend fun shareImage(file: PlatformFile): Boolean = withContext(Dispatchers.Main) {
    val window = NSApplication.sharedApplication.mainWindow ?: NSApplication.sharedApplication.keyWindow ?: return@withContext false
    val contentView = window.contentView ?: return@withContext false

    val locationInWindow = NSApplication.sharedApplication.currentEvent?.locationInWindow ?: return@withContext false

    val picker = NSSharingServicePicker(items = listOf(file.nsUrl))

    val rect = locationInWindow.useContents {
        NSMakeRect(x, y, 1.0, 1.0)
    }

    picker.showRelativeToRect(
        rect,
        ofView = contentView,
        preferredEdge = NSRectEdgeMinY
    )

    return@withContext true
}
