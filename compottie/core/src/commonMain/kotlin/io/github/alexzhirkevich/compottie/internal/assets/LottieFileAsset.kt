package io.github.alexzhirkevich.compottie.internal.assets

public sealed interface LottieFileAsset : LottieAsset {
    public val path: String
    public val fileName: String?
    public val embedded: Boolean

    public suspend fun prepare()
}