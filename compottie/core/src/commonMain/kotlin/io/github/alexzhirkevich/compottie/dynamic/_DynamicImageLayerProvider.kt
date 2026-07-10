package io.github.alexzhirkevich.compottie.dynamic

import androidx.compose.ui.graphics.ImageBitmap
import io.github.alexzhirkevich.compottie.assets.LottieImageSpec
import io.github.alexzhirkevich.compottie.internal.AnimationState

@PublishedApi
internal class DynamicImageLayerProvider() : DynamicLayerProvider(), DynamicImageLayer {

    var image: (AnimationState.(source: LottieImageSpec?) -> ImageBitmap)? = null
        private set

    override fun image(image: AnimationState.(source: LottieImageSpec?) -> ImageBitmap) {
        this.image = image
    }
}
