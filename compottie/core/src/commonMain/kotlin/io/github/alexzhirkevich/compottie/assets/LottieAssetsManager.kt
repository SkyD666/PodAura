package io.github.alexzhirkevich.compottie.assets

import androidx.compose.runtime.Stable

/**
 * Used to fetch lottie assets that are not embedded to the animation JSON file
 * */
@Stable
public interface LottieAssetsManager {

    /**
     * Load image asset
     *
     * @param id unique asset id that is used for referring from animation layers
     * @param path relative system path or web URL excluding file name. For example:
     *
     * - /path/to/images/
     * - https://example.com/images/
     *
     * @param name asset name and extension (for example image.png)
     * */
    public suspend fun image(image: LottieImageSpec): ImageRepresentable?
}

internal object EmptyAssetsManager : LottieAssetsManager {
    override suspend fun image(image: LottieImageSpec): ImageRepresentable? {
        return null
    }
}

