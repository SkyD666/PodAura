package com.skyd.podaura.ui.screen.nano

import androidx.compose.runtime.Composable

/** Page content supplied by the application. Routes and navigation remain in shared. */
class NanoPages(
    val available: Boolean,
    val title: suspend () -> String,
    val chat: @Composable (callbacks: NanoChatCallbacks) -> Unit,
    val settings: @Composable (onBack: () -> Unit) -> Unit,
)
