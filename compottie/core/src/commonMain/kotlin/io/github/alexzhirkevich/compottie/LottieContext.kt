package io.github.alexzhirkevich.compottie

import androidx.compose.runtime.Composable

@InternalCompottieApi
public expect abstract class LottieContext

@Composable
@InternalCompottieApi
public expect fun currentLottieContext(): LottieContext
