package io.github.alexzhirkevich.compottie.internal.helpers.text

import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedVector2
import io.github.alexzhirkevich.compottie.internal.animation.ExpressionHolder
import kotlinx.serialization.Serializable

@Serializable
internal class TextAlignment(
    val alignment: AnimatedVector2? = null,
    val grouping: TextGrouping? = null
) : ExpressionHolder {
    fun copy() = TextAlignment(
        alignment = alignment?.copy(),
        grouping = grouping
    )

    override fun prepareExpressions(state: AnimationState) {
        alignment?.prepareExpressions(state)
    }
}