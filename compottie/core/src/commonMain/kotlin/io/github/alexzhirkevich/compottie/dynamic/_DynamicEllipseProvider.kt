package io.github.alexzhirkevich.compottie.dynamic

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

internal class DynamicEllipseProvider : DynamicShapeProvider(), DynamicEllipse {

    var size: PropertyProvider<Size>? = null
        private set

    var position: PropertyProvider<Offset>? = null
        private set

    override fun size(provider: PropertyProvider<Size>) {
        size = provider
    }

    override fun position(provider: PropertyProvider<Offset>) {
        position = provider
    }
}
