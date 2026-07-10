package io.github.alexzhirkevich.compottie.dynamic

public sealed interface DynamicLayer {

    public fun hidden(provider: PropertyProvider<Boolean>)

    public fun transform(builder: DynamicTransform.() -> Unit)
}




