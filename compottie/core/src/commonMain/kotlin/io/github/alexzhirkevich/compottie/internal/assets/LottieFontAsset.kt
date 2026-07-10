package io.github.alexzhirkevich.compottie.internal.assets

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import io.github.alexzhirkevich.compottie.assets.LottieFontSpec
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.jvm.JvmInline

@Serializable
internal class FontList(
    val list: List<LottieFontAsset>
) {

    @Transient
    private val map = list.flatMap {
        listOf(it.name to it.spec, it.family to it.spec)
    }.toMap()

    fun find(family: String): LottieFontSpec? {
        return map[family]
    }

    fun deepCopy(): FontList {
        return FontList(list.map(LottieFontAsset::copy))
    }
}

@Serializable
internal class LottieFontAsset(

    @SerialName("fFamily")
    val family: String,

    @SerialName("fName")
    val name: String,

    @SerialName("fStyle")
    val style: String,

    @SerialName("fPath")
    val path: String? = null,

    @SerialName("origin")
    val origin: FontOrigin? = null,

    @SerialName("ascent")
    val ascent: Float = 0f
) {


    @Transient
    private val lStyle = style.lowercase()

    @Transient
    val weight = when {
        "extra" in lStyle && "light" in lStyle -> FontWeight.ExtraLight
        "light" in lStyle -> FontWeight.Light
        "extra" in lStyle && "bold" in lStyle -> FontWeight.ExtraBold
        "semi" in style && "bold" in lStyle -> FontWeight.SemiBold
        "bold" in lStyle -> FontWeight.Bold
        "black" in lStyle -> FontWeight.Black
        else -> FontWeight.Normal
    }

    @Transient
    val fontStyle = if ("italic" in lStyle)
        FontStyle.Italic else FontStyle.Normal

    var font: Font? = null

    val spec by lazy {
        LottieFontSpec(
            family = family,
            name = name,
            style = fontStyle,
            weight = weight,
            path = path,
            origin = origin?.toSpecOrigin() ?: LottieFontSpec.FontOrigin.Unknown,
            ascent = ascent
        )
    }

    fun copy(): LottieFontAsset {
        return LottieFontAsset(
            family = family,
            name = name,
            style = style,
            path = path,
            origin = origin,
            ascent = ascent
        )
    }
}

@JvmInline
@Serializable
internal value class FontOrigin(val type: Byte) {
    companion object {
        val Local = FontOrigin(0)
        val CssUrl = FontOrigin(1)
        val ScriptUrl = FontOrigin(2)
        val FontUrl = FontOrigin(3)
    }

    fun toSpecOrigin() = when (this) {
        Local -> LottieFontSpec.FontOrigin.Local
        CssUrl -> LottieFontSpec.FontOrigin.CssUrl
        ScriptUrl -> LottieFontSpec.FontOrigin.ScriptUrl
        FontUrl -> LottieFontSpec.FontOrigin.FontUrl
        else -> LottieFontSpec.FontOrigin.Unknown
    }
}