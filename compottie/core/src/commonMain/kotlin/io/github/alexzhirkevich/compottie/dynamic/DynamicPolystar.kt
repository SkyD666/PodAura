package io.github.alexzhirkevich.compottie.dynamic

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

public interface DynamicPolystar : DynamicShape {

    public fun size(provider: PropertyProvider<Size>)

    public fun position(provider: PropertyProvider<Offset>)

    public fun rotation(provider: PropertyProvider<Float>)

    public fun points(provider: PropertyProvider<Float>)

    public fun innerRoundness(provider: PropertyProvider<Float>)

    public fun innerRadius(provider: PropertyProvider<Float>)

    public fun outerRadius(provider: PropertyProvider<Float>)

    public fun outerRoundness(provider: PropertyProvider<Float>)
}