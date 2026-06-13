package com.skyd.podaura.ext

import androidx.compose.ui.platform.Clipboard
import io.github.vinceglb.filekit.PlatformFile
import platform.AppKit.NSImage
import platform.AppKit.NSPasteboard

actual suspend fun Clipboard.setImage(file: PlatformFile, mimeType: String) {
    val image = NSImage(contentsOfURL = file.nsUrl)
    val pasteboard = NSPasteboard.generalPasteboard
    pasteboard.clearContents()
    pasteboard.writeObjects(listOf(image))
}
