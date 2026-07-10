package io.github.alexzhirkevich.compottie.internal.animation

import io.github.alexzhirkevich.keight.js.JsAny

public interface PropertyGroup : JsAny {
    public val group: PropertyGroup?
}