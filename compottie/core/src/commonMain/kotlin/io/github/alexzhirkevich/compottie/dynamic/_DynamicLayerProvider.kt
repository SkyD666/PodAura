package io.github.alexzhirkevich.compottie.dynamic

internal open class DynamicLayerProvider : DynamicLayer {

    var transform: DynamicTransformProvider? = null

    var hidden: PropertyProvider<Boolean>? = null
        private set

    override fun hidden(provider: PropertyProvider<Boolean>) {
        hidden = provider
    }

    override fun transform(builder: DynamicTransform.() -> Unit) {
        transform = DynamicTransformProvider().apply(builder)
    }
}