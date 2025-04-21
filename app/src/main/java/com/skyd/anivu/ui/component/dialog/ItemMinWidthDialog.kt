package com.skyd.anivu.ui.component.dialog

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WidthNormal
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.skyd.anivu.R

@Composable
fun ItemMinWidthDialog(
    onDismissRequest: () -> Unit,
    initValue: Float,
    defaultValue: () -> Float,
    valueRange: ClosedFloatingPointRange<Float> = 200f..1000f,
    onConfirm: (Float) -> Unit,
) {
    SliderWithLabelDialog(
        onDismissRequest = onDismissRequest,
        initValue = initValue,
        defaultValue = defaultValue,
        valueRange = valueRange,
        icon = Icons.Outlined.WidthNormal,
        title = stringResource(id = R.string.min_width_dp),
        label = { "%.2f".format(it) + " dp" },
        onConfirm = onConfirm,
    )
}