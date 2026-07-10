package io.github.alexzhirkevich.compottie.dynamic

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

public interface DynamicRect : DynamicShape {

    public fun size(provider: PropertyProvider<Size>)

    public fun position(provider: PropertyProvider<Offset>)

    public fun roundCorners(provider: PropertyProvider<Float>)
}

