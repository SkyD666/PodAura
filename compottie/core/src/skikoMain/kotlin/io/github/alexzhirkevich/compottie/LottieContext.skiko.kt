package io.github.alexzhirkevich.compottie

import androidx.compose.runtime.Composable

@InternalCompottieApi
public actual abstract class LottieContext {

    internal companion object : LottieContext()
}

@Composable
@InternalCompottieApi
public actual fun currentLottieContext(): LottieContext = LottieContext
