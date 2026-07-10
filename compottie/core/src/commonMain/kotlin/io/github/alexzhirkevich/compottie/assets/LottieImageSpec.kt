package io.github.alexzhirkevich.compottie.assets

import androidx.compose.runtime.Immutable

@Immutable
public class LottieImageSpec internal constructor(
    public val id: String,
    public val path: String,
    public val name: String,
    public val width: Int,
    public val height: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as LottieImageSpec

        if (id != other.id) return false
        if (path != other.path) return false
        if (name != other.name) return false
        if (width != other.width) return false
        if (height != other.height) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        return result
    }

    override fun toString(): String {
        return "LottieImageSpec(id='$id', path='$path', name='$name', width=$width, height=$height)"
    }
}