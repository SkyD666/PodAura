package com.skyd.podaura.ext

import androidx.compose.ui.platform.Clipboard
import io.github.vinceglb.filekit.PlatformFile
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIImage
import platform.UIKit.UIPasteboard

actual suspend fun Clipboard.setImage(file: PlatformFile, mimeType: String) {
    val data = NSData.dataWithContentsOfURL(file.nsUrl) ?: error("Unable to load image data")
    UIPasteboard.generalPasteboard.image = UIImage(data)
}
