package io.github.alexzhirkevich.compottie.internal.platform.effects

import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.skiaPaint

internal actual fun Paint.resetEffects() {
    skiaPaint.imageFilter = null
}
