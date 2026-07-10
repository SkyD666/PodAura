package io.github.alexzhirkevich.compottie.dynamic

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import io.github.alexzhirkevich.compottie.internal.helpers.text.TextJustify

public interface DynamicTextLayer : DynamicLayer {

    /**
     * Dynamically provide text content for this layer.
     */
    public fun text(provider: PropertyProvider<String>)

    public fun fontSize(provider: PropertyProvider<Float>)

    public fun lineHeight(provider: PropertyProvider<Float>)

    public fun lineSpacing(provider: PropertyProvider<Float>)

    public fun fillColor(provider: PropertyProvider<Color>)

    public fun strokeColor(provider: PropertyProvider<Color>)

    public fun strokeWidth(provider: PropertyProvider<Float>)

    /**
     * If stroke should be drawn before fill
     * */
    public fun strokeOverFill(provider: PropertyProvider<Boolean>)

    public fun textJustify(provider: PropertyProvider<TextJustify>)

    public fun baselineShift(provider: PropertyProvider<Float>)

    public fun tracking(provider: PropertyProvider<Float>)

    public fun size(provider: PropertyProvider<Size>)

    public fun position(provider: PropertyProvider<Offset>)
}

