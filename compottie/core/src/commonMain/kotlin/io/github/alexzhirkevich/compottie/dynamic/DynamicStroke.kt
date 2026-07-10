package io.github.alexzhirkevich.compottie.dynamic

public sealed interface DynamicStroke : DynamicDraw {
    public fun width(provider: PropertyProvider<Float>) {}
}

