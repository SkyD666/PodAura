package io.github.alexzhirkevich.compottie.internal.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorInfo
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Pixmap
import org.jetbrains.skia.SamplingMode

internal actual suspend fun ImageBitmap.Companion.fromBytes(
    bytes: ByteArray,
    width: Int,
    height: Int
): ImageBitmap {
    val image = Image.makeFromEncoded(bytes)

    if (image.width == width && image.height == height)
        return image.toComposeImageBitmap()

    val imageInfo = ImageInfo(
        colorInfo = ColorInfo(
            ColorType.RGBA_8888,
            ColorAlphaType.UNPREMUL,
            ColorSpace.sRGB,
        ),
        width = width,
        height = height
    )

    val pixMap = Pixmap.make(
        info = imageInfo,
        buffer = Data.makeUninitialized(
            imageInfo.computeByteSize(imageInfo.minRowBytes)
        ),
        rowBytes = imageInfo.minRowBytes
    )

    image.scalePixels(pixMap, SamplingMode.LINEAR, false)

    return Image
        .makeFromPixmap(pixMap)
        .toComposeImageBitmap()
}
