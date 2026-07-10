package io.github.alexzhirkevich.compottie.dynamic

import androidx.compose.ui.graphics.ImageBitmap
import io.github.alexzhirkevich.compottie.assets.LottieImageSpec
import io.github.alexzhirkevich.compottie.internal.AnimationState

public interface DynamicImageLayer : DynamicLayer {

    /**
     * Dynamically provide image for this layer.
     *
     * Image must be exact same size as required in spec (if spec is not null).
     *
     * Note: [image] block will be called for each frame so bitmap MUST BE CACHED
     * and must be initialized OUTSIDE of this block.
     *
     * You can also use [AnimationState.images] to access all bitmaps loaded for this animation
     *
     * Example:
     *
     * ```kotlin
     * imageLayer("Image 1"){
     *   val image1 = //... you can init bitmaps here.
     *   val image2 = //... it will be invoked 1 time but in the UI thread
     *
     *   image { spec ->
     *      // do NOT init or manipulate bitmaps here!!! only ready instances
     *      if (progress > .5f) image1 else image2
     *
     *      // this is AnimationState receiver context
     *      // so you can use [AnimationState.images] here
     *      if (progress > .5f) images["image_2"]!! else images[spec.id]!!
     *   }
     * }
     * ```
     */
    public fun image(image: AnimationState.(spec: LottieImageSpec?) -> ImageBitmap)
}

