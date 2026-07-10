package io.github.alexzhirkevich.compottie.dynamic

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ScaleFactor

public interface DynamicTransform {

    /**
     * Apply dynamic scale transform for layer.
     *
     * Scale x and y must be in range (0..1)
     *
     * Note: source is the relative layer scale derived from the layer transform (not absolute value).
     * Scale derived from this provide will be multiplied by parent layer scale
     * */
    public fun scale(provider: PropertyProvider<ScaleFactor>)

    /**
     * Apply dynamic offset transform for layer.
     *
     * Note: source is the relative layer offset derived from the layer transform (not absolute value).
     * Offset derived from this provider will be concatenated with parent layer offset
     * */
    public fun offset(provider: PropertyProvider<Offset>)

    /**
     * Apply dynamic rotation transform for layer.
     *
     * Note: source is the relative layer rotation derived from the layer transform (not absolute value).
     * Rotation derived from this provider will be concatenated with parent layer rotation
     * */
    public fun rotation(provider: PropertyProvider<Float>)

    /**
     * Apply dynamic skew transform for layer.
     *
     * Note: source is the relative layer skew derived from the layer transform (not absolute value).
     * Skew derived from this provider will be concatenated with parent layer skew
     * */
    public fun skew(provider: PropertyProvider<Float>)

    /**
     * Angle of the [skew]
     *
     * Note: source is the relative layer skew angle derived from the layer transform (not absolute value).
     * */
    public fun skewAxis(provider: PropertyProvider<Float>)

    /**
     * Apply dynamic opacity transform
     *
     * Opacity must be in range (0..1)
     *
     * Note: source is the relative layer opacity derived from the layer transform (not absolute value).
     * Opacity derived from this provider will be multiplied by parent layer opacity
     * */
    public fun opacity(provider: PropertyProvider<Float>)
}
