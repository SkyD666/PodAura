package com.skyd.podaura.ext

import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.asAwtTransferable
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.awt.Image
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.time.Duration.Companion.milliseconds

private const val CLIPBOARD_READ_ATTEMPTS = 5
private const val CLIPBOARD_RETRY_DELAY_MILLIS = 10L

actual suspend fun Clipboard.setImage(file: PlatformFile, mimeType: String) {
    setClipEntry(ClipEntry(ImageTransferable(withContext(Dispatchers.IO) {
        ImageIO.read(file.file)
    })))
}

actual suspend fun Clipboard.readText(): String? = withContext(Dispatchers.IO) {
    repeat(CLIPBOARD_READ_ATTEMPTS) { attempt ->
        try {
            val transferable = getClipEntry()?.asAwtTransferable ?: return@withContext null
            return@withContext transferable.getPlainText()
        } catch (_: IllegalStateException) {
            if (attempt == CLIPBOARD_READ_ATTEMPTS - 1) return@withContext null
            delay(CLIPBOARD_RETRY_DELAY_MILLIS.milliseconds)
        }
    }
    null
}

internal fun Transferable.getPlainText(): String? {
    if (!isDataFlavorSupported(DataFlavor.stringFlavor)) return null
    return try {
        getTransferData(DataFlavor.stringFlavor).toString()
    } catch (_: UnsupportedFlavorException) {
        null
    } catch (_: IOException) {
        null
    }
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
