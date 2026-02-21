package com.skyd.podaura.ext

import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import io.github.vinceglb.filekit.PlatformFile
import java.awt.Image
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import javax.imageio.ImageIO

actual suspend fun Clipboard.setImage(file: PlatformFile, mimeType: String) {
    setClipEntry(ClipEntry(ImageTransferable(ImageIO.read(file.file))))
}

private class ImageTransferable(private val image: Image) : Transferable {
    override fun getTransferDataFlavors() = arrayOf(DataFlavor.imageFlavor)
    override fun isDataFlavorSupported(flavor: DataFlavor) = flavor == DataFlavor.imageFlavor
    override fun getTransferData(flavor: DataFlavor): Any {
        if (isDataFlavorSupported(flavor)) {
            return image
        } else {
            throw UnsupportedFlavorException(flavor)
        }
    }
}