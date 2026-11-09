package com.skyd.podaura.ui.screen.podnya

import androidx.compose.runtime.Composable

/** Page content supplied by the application. Routes and navigation remain in shared. */
class PodnyaPages(
    val available: Boolean,
    val title: suspend () -> String,
    val chat: @Composable (callbacks: PodnyaChatCallbacks) -> Unit,
    val settings: @Composable (onBack: () -> Unit) -> Unit,
)
