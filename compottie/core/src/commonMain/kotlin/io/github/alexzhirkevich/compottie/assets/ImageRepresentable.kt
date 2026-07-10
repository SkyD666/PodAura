package io.github.alexzhirkevich.compottie.assets

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import io.github.alexzhirkevich.compottie.internal.assets.resize
import io.github.alexzhirkevich.compottie.internal.platform.fromBytes
import kotlin.jvm.JvmInline
import androidx.compose.ui.graphics.painter.Painter as ComposePainter

public interface ImageRepresentable {

    public suspend fun toBitmap(width: Int, height: Int): ImageBitmap

    @JvmInline
    public value class Bytes(private val bytes: ByteArray) : ImageRepresentable {

        override suspend fun toBitmap(width: Int, height: Int): ImageBitmap {
            return ImageBitmap.fromBytes(bytes, width, height)
        }
    }

    @JvmInline
    public value class Painter(private val painter: ComposePainter) : ImageRepresentable {
        override suspend fun toBitmap(width: Int, height: Int): ImageBitmap {
            return painter.toBitmap(width, height)
        }
    }

    @JvmInline
    public value class Bitmap(private val bitmap: ImageBitmap) : ImageRepresentable {
        override suspend fun toBitmap(width: Int, height: Int): ImageBitmap {
            return bitmap.resize(width, height)
        }
    }
}

private fun ComposePainter.toBitmap(w: Int, h: Int): ImageBitmap {

    val bmp = ImageBitmap(w, h)
    val canvas = Canvas(bmp)

    CanvasDrawScope().draw(
        density = Density(1f, 1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = canvas,
        size = Size(w.toFloat(), h.toFloat())
    ) {
        draw(this@draw.size)
    }

    return bmp
}