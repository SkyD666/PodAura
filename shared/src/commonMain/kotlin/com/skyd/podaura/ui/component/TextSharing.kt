package com.skyd.podaura.ui.component

import androidx.compose.runtime.Composable

interface TextSharing {
    fun share(text: String)
}

@Composable
expect fun rememberTextSharing(): TextSharing