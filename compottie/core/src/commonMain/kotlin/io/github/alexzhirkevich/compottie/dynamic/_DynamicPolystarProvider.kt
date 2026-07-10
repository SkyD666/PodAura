package io.github.alexzhirkevich.compottie.dynamic

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

internal class DynamicPolystarProvider : DynamicShapeProvider(), DynamicPolystar {

    var size: PropertyProvider<Size>? = null
        private set

    var position: PropertyProvider<Offset>? = null
        private set

    var rotation: PropertyProvider<Float>? = null
        private set

    var points: PropertyProvider<Float>? = null
        private set

    var innerRoundness: PropertyProvider<Float>? = null
        private set

    var innerRadius: PropertyProvider<Float>? = null
        private set

    var outerRadius: PropertyProvider<Float>? = null
        private set

    var outerRoundness: PropertyProvider<Float>? = null
        private set

    override fun size(provider: PropertyProvider<Size>) {
        size = provider
    }

    override fun position(provider: PropertyProvider<Offset>) {
        position = provider
    }

    override fun rotation(provider: PropertyProvider<Float>) {
        rotation = provider
    }

    override fun points(provider: PropertyProvider<Float>) {
        points = provider
    }

    override fun innerRoundness(provider: PropertyProvider<Float>) {
        innerRoundness = provider
    }

    override fun innerRadius(provider: PropertyProvider<Float>) {
        innerRadius = provider
    }

    override fun outerRadius(provider: PropertyProvider<Float>) {
        outerRadius = provider
    }

    override fun outerRoundness(provider: PropertyProvider<Float>) {
        outerRoundness = provider
    }
}