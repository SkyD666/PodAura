package io.github.alexzhirkevich.compottie

public open class CompottieException @InternalCompottieApi constructor(
    msg: String,
    cause: Throwable? = null
) : Exception(msg, cause)
