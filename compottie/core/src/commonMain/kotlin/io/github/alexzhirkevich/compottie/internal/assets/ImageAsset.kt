package io.github.alexzhirkevich.compottie.internal.assets

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntSize
import io.github.alexzhirkevich.compottie.assets.LottieImageSpec
import io.github.alexzhirkevich.compottie.internal.helpers.BooleanIntSerializer
import io.github.alexzhirkevich.compottie.internal.platform.fromBytes
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.io.encoding.Base64

@Serializable
public class ImageAsset(

    @SerialName("id")
    override val id: String,

    @SerialName("p")
    override val fileName: String,

    @SerialName("u")
    override val path: String = "",

    @SerialName("nm")
    public val name: String? = null,

    @SerialName("e")
    @Serializable(with = BooleanIntSerializer::class)
    override val embedded: Boolean = false,

    @SerialName("w")
    private val w: Int? = null,

    @SerialName("h")
    private val h: Int? = null,

    internal val sid: String? = null,
) : LottieFileAsset {

    public val width: Int get() = w ?: bitmap?.width ?: 0

    public val height: Int get() = h ?: bitmap?.height ?: 0

    @Transient
    internal val spec = LottieImageSpec(
        id = id,
        path = path,
        name = fileName,
        width = width,
        height = height
    )

    @Transient
    internal var bitmap: ImageBitmap? = null
        private set

    override suspend fun prepare() {
        if (bitmap == null) {
            fileName
                .takeIf { embedded || it.isBase64Data }
                ?.substringAfter("base64,")
                ?.trim()
                ?.let {
                    runCatching {
                        setBitmap(ImageBitmap.fromBytes(Base64.decode(it), width, height))
                    }
                }
        }
    }

    internal fun setBitmap(bitmap: ImageBitmap) {
        this.bitmap = bitmap
    }

    override fun copy(): LottieAsset =
        ImageAsset(
            id = id,
            fileName = fileName,
            path = path,
            name = name,
            embedded = embedded,
            w = w,
            h = h
        ).apply {
            setBitmap(this@ImageAsset.bitmap ?: return@apply)
        }
}

private val String.isBase64Data: Boolean
    get() = (startsWith("data:") && contains("base64,"))


private val emptyPaint = Paint()

internal fun ImageBitmap.resize(w: Int, h: Int): ImageBitmap {
    if (width == w && height == h) {
        return this
    }
    val bitmap = ImageBitmap(w, h)

    Canvas(bitmap).apply {
        drawImageRect(
            image = this@resize,
            dstSize = IntSize(w - 1, h - 1),
            paint = emptyPaint
        )
    }

    return bitmap
}

