package io.github.alexzhirkevich.compottie.internal.effects

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.util.fastForEachReversed
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.layers.BaseLayer
import io.github.alexzhirkevich.compottie.internal.platform.effects.PlatformDropShadowEffect

internal class LayerEffectsApplier(
    private val layer: BaseLayer
) {
    fun applyTo(paint: Paint, animationState: AnimationState, effectState: LayerEffectsState) {

        layer.effects.fastForEachReversed {
            it.apply(paint, animationState, effectState)
        }

        effectState.lastPaint = paint
    }
}

internal class LayerEffectsState {
    var blurRadius: Float? = null
    var lastPaint: Paint? = null

    var lastFillColor: Color? = null
    var lastFillFilter: ColorFilter? = null

    var dropShadowHash: Int? = null
    var dropShadowEffect: PlatformDropShadowEffect? = null

    var tintHash: Int? = null
    var tintColorFiter: ColorFilter? = null
}
