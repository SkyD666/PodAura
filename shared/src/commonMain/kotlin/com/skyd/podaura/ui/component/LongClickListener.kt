package com.skyd.podaura.ui.component

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalViewConfiguration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LongClickListener(
    interactionSource: InteractionSource,
    onLongClick: (() -> Unit)?,
    onClick: () -> Unit,
) {
    val viewConfiguration = LocalViewConfiguration.current
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val currentOnClick by rememberUpdatedState(onClick)

    LaunchedEffect(interactionSource) {
        var isLongClick = false

        interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    currentOnLongClick?.let { currentOnLongClick ->
                        isLongClick = false
                        delay(viewConfiguration.longPressTimeoutMillis)
                        isLongClick = true
                        currentOnLongClick()
                    }
                }

                is PressInteraction.Release -> {
                    if (!isLongClick) {
                        currentOnClick()
                    }
                }
            }
        }
    }
}