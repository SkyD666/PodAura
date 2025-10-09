package com.skyd.podaura.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
fun TopSnackbatHostBox(snackbarHost: @Composable () -> Unit) {
    Popup(
        alignment = Alignment.BottomCenter,
        properties = PopupProperties(clippingEnabled = false),
    ) {
        snackbarHost()
    }
}