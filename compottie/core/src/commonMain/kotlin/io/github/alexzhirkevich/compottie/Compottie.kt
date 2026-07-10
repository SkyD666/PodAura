package io.github.alexzhirkevich.compottie

public object Compottie {

    public const val IterateForever: Int = Int.MAX_VALUE

    /**
     * Logger used to inform about various events, errors, unsupported features, etc.
     *
     * Default instance uses stdout / stderr.
     * You can set it to null for production
     * */
    public var logger: LottieLogger? = LottieLogger.Default
}
