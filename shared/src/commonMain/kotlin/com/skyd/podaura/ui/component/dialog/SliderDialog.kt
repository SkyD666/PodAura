package com.skyd.podaura.ui.component.dialog

import androidx.annotation.IntRange
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SliderDialog(
    onDismissRequest: () -> Unit,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    @IntRange(from = 0) steps: Int = 0,
    valueLabel: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
        )
    },
    title: @Composable (() -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
) {
    PodAuraDialog(
        onDismissRequest = onDismissRequest,
        icon = icon,
        title = title,
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (valueLabel != null) {
                    valueLabel()
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = valueRange,
                    steps = steps,
                )
            }
        },
        selectable = false,
        scrollable = false,
        confirmButton = confirmButton,
        dismissButton = dismissButton
    )
}