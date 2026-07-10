package io.github.alexzhirkevich.compottie.dynamic

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ScaleFactor

internal class DynamicTransformProvider : DynamicTransform {

    var scale: PropertyProvider<ScaleFactor>? = null
        private set
    var offset: PropertyProvider<Offset>? = null
        private set
    var rotation: PropertyProvider<Float>? = null
        private set
    var opacity: PropertyProvider<Float>? = null
        private set
    var skew: PropertyProvider<Float>? = null
        private set
    var skewAxis: PropertyProvider<Float>? = null
        private set

    override fun scale(provider: PropertyProvider<ScaleFactor>) {
        this.scale = provider
    }

    override fun offset(provider: PropertyProvider<Offset>) {
        this.offset = provider
    }

    override fun rotation(provider: PropertyProvider<Float>) {
        this.rotation = provider
    }

    override fun opacity(provider: PropertyProvider<Float>) {
        this.opacity = provider
    }

    override fun skew(provider: PropertyProvider<Float>) {
        this.skew = provider
    }

    override fun skewAxis(provider: PropertyProvider<Float>) {
        this.skewAxis = provider
    }
}