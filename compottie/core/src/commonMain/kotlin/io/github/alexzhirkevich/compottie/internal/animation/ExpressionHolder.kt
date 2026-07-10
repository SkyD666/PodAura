package io.github.alexzhirkevich.compottie.internal.animation

import io.github.alexzhirkevich.compottie.internal.AnimationState

public interface ExpressionHolder {

    public fun prepareExpressions(state: AnimationState)
}