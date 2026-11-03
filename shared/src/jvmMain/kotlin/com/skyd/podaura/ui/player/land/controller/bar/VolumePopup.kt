package com.skyd.podaura.ui.player.land.controller.bar

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.PopupProperties

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun volumePopupProperties(focusable: Boolean): PopupProperties = PopupProperties(
    focusable = focusable,
    consumePointerInputOutside = false,
)
