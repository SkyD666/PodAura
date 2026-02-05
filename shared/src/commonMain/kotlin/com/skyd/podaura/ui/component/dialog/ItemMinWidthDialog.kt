package com.skyd.podaura.ui.component.dialog

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WidthNormal
import androidx.compose.runtime.Composable
import com.skyd.compone.component.dialog.SliderWithLabelDialog
import com.skyd.fundation.ext.format
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.min_width_dp

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
        title = stringResource(Res.string.min_width_dp),
        label = { it.format(2) + " dp" },
        onConfirm = onConfirm,
    )
}