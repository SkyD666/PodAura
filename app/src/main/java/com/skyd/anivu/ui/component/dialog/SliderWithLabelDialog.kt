package com.skyd.anivu.ui.component.dialog

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.skyd.anivu.R
import com.skyd.anivu.ui.component.PodAuraIconButton

@Composable
fun SliderWithLabelDialog(
    onDismissRequest: () -> Unit,
    initValue: Float,
    defaultValue: () -> Float,
    valueRange: ClosedFloatingPointRange<Float>,
    icon: ImageVector,
    title: String,
    label: (Float) -> String,
    onConfirm: (Float) -> Unit,
) {
    var value by rememberSaveable { mutableFloatStateOf(initValue) }

    SliderDialog(
        onDismissRequest = onDismissRequest,
        value = value,
        onValueChange = { value = it },
        valueRange = valueRange,
        valueLabel = {
            Box(modifier = Modifier.Companion.fillMaxWidth()) {
                Text(
                    modifier = Modifier.Companion
                        .align(Alignment.Companion.Center)
                        .animateContentSize(),
                    text = label(value),
                    style = MaterialTheme.typography.titleMedium,
                )
                PodAuraIconButton(
                    modifier = Modifier.Companion.align(Alignment.Companion.CenterEnd),
                    onClick = { value = defaultValue() },
                    imageVector = Icons.Outlined.Restore,
                    contentDescription = stringResource(R.string.reset),
                )
            }
        },
        icon = { Icon(imageVector = icon, contentDescription = null) },
        title = { Text(text = title) },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) {
                Text(text = stringResource(id = R.string.ok))
            }
        }
    )
}