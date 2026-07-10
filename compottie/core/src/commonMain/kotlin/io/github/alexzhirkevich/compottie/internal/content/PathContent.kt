package io.github.alexzhirkevich.compottie.internal.content

import androidx.compose.ui.graphics.Path
import io.github.alexzhirkevich.compottie.internal.AnimationState

internal interface PathContent : Content {
    fun getPath(state: AnimationState): Path
}