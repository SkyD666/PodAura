package io.github.alexzhirkevich.compottie

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

public actual typealias LottieContext = Context

@Composable
@InternalCompottieApi
public actual fun currentLottieContext(): LottieContext {
    return LocalContext.current
}
