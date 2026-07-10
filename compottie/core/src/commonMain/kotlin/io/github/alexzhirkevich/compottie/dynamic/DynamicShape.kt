package io.github.alexzhirkevich.compottie.dynamic

public sealed interface DynamicShape {

    public fun hidden(provider: PropertyProvider<Boolean>)
}