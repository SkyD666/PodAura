package com.skyd.podaura.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.window.WindowScope
import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.platform
import com.sun.jna.WString
import com.sun.jna.platform.win32.Shell32
import java.awt.Taskbar
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Gives Gradle/JVM launches their own taskbar identity instead of java.exe's identity. */
internal fun initWindowsAppIdentity() {
    if (platform != Platform.Windows) return

    Shell32.INSTANCE.SetCurrentProcessExplicitAppUserModelID(WString("com.skyd.podaura"))
    runCatching {
        val taskbar = Taskbar.getTaskbar()
        if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
            taskbar.iconImage = PodAuraIcons.images.last()
        }
    }
}

/** Applies all sizes from the packaged PodAura.ico to a Compose/AWT window. */
@Composable
internal fun WindowScope.ApplyPodAuraWindowIcon() {
    if (platform != Platform.Windows) return

    DisposableEffect(window) {
        val previousIcons = window.iconImages
        window.iconImages = PodAuraIcons.images
        onDispose { window.iconImages = previousIcons }
    }
}

private object PodAuraIcons {
    val images: List<BufferedImage> by lazy {
        val bytes = checkNotNull(PodAuraIcons::class.java.getResourceAsStream("/PodAura.ico")) {
            "Missing JVM resource /PodAura.ico"
        }.use { it.readBytes() }
        decodeIco(bytes)
    }

    /** Decodes the uncompressed 32-bit DIB entries contained in PodAura.ico. */
    private fun decodeIco(bytes: ByteArray): List<BufferedImage> {
        val data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        check(data.getShort(2).toInt() == 1) { "PodAura.ico is not an icon file" }
        val count = data.getShort(4).toInt() and 0xFFFF

        return buildList {
            repeat(count) { index ->
                val entry = 6 + index * 16
                val width = (bytes[entry].toInt() and 0xFF).let { if (it == 0) 256 else it }
                val height = (bytes[entry + 1].toInt() and 0xFF).let { if (it == 0) 256 else it }
                val imageOffset = data.getInt(entry + 12)
                val headerSize = data.getInt(imageOffset)
                val storedHeight = data.getInt(imageOffset + 8)
                val bitsPerPixel = data.getShort(imageOffset + 14).toInt() and 0xFFFF
                val compression = data.getInt(imageOffset + 16)
                check(headerSize >= 40 && bitsPerPixel == 32 && compression == 0) {
                    "Unsupported PodAura.ico entry: ${width}x$height, $bitsPerPixel-bit"
                }

                val pixelsOffset = imageOffset + headerSize
                val rowStride = width * 4
                val bottomUp = storedHeight > 0
                // Windows AWT converts window icons through a premultiplied surface. Supplying
                // straight-alpha pixels here makes translucent white pixels turn cyan.
                val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE)
                for (y in 0 until height) {
                    val sourceY = if (bottomUp) height - 1 - y else y
                    for (x in 0 until width) {
                        val pixel = pixelsOffset + sourceY * rowStride + x * 4
                        val blue = bytes[pixel].toInt() and 0xFF
                        val green = bytes[pixel + 1].toInt() and 0xFF
                        val red = bytes[pixel + 2].toInt() and 0xFF
                        val alpha = bytes[pixel + 3].toInt() and 0xFF
                        image.setRGB(
                            x,
                            y,
                            (alpha shl 24) or (red shl 16) or (green shl 8) or blue,
                        )
                    }
                }
                add(image)
            }
        }.sortedBy { it.width }
    }
}
