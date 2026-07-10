package io.github.alexzhirkevich.compottie.dynamic

internal open class DynamicShapeProvider : DynamicShape {

    var hidden: PropertyProvider<Boolean>? = null
        private set

    override fun hidden(provider: PropertyProvider<Boolean>) {
        hidden = provider
    }
}