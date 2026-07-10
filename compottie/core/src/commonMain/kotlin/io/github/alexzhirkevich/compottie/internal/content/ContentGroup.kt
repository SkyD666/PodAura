package io.github.alexzhirkevich.compottie.internal.content

import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedTransform

internal interface ContentGroup : DrawingContent, PathContent {

    val transform: AnimatedTransform?

    val isEmpty: Boolean

    fun hidden(state: AnimationState): Boolean
}