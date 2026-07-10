package io.github.alexzhirkevich.compottie.dynamic

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

public interface DynamicEllipse : DynamicShape {

    public fun size(provider: PropertyProvider<Size>)

    public fun position(provider: PropertyProvider<Offset>)
}